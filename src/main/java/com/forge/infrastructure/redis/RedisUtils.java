package com.forge.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Central access point for the optional Redis infrastructure features. */
@Configuration
public class RedisUtils implements WebMvcConfigurer {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean cacheEnabled;
    private final Duration cacheTtl;
    private final boolean rateLimitEnabled;
    private final int rateLimit;
    private final Duration rateLimitWindow;

    public RedisUtils(StringRedisTemplate redis, ObjectMapper objectMapper,
            boolean cacheEnabled, long cacheTtlSeconds, boolean rateLimitEnabled,
            int rateLimit, long rateLimitWindowSeconds) {
        if (cacheTtlSeconds < 1)
            throw new IllegalArgumentException("Cache TTL must be positive");
        if (rateLimit < 1 || rateLimitWindowSeconds < 1)
            throw new IllegalArgumentException("Rate-limit settings are invalid");
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.cacheEnabled = cacheEnabled;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimit = rateLimit;
        this.rateLimitWindow = Duration.ofSeconds(rateLimitWindowSeconds);
    }

    @Autowired
    public RedisUtils(ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            @Value("${forge.redis.cache.enabled:false}") boolean cacheEnabled,
            @Value("${forge.redis.cache.ttl-seconds:300}") long cacheTtlSeconds,
            @Value("${forge.redis.rate-limit.enabled:false}") boolean rateLimitEnabled,
            @Value("${forge.redis.rate-limit.limit:120}") int rateLimit,
            @Value("${forge.redis.rate-limit.window-seconds:60}") long rateLimitWindowSeconds) {
        if (cacheTtlSeconds < 1)
            throw new IllegalArgumentException("Cache TTL must be positive");
        if (rateLimit < 1 || rateLimitWindowSeconds < 1)
            throw new IllegalArgumentException("Rate-limit settings are invalid");
        this.redis = redisProvider.getIfAvailable();
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        this.cacheEnabled = cacheEnabled;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimit = rateLimit;
        this.rateLimitWindow = Duration.ofSeconds(rateLimitWindowSeconds);
    }

    public RedisUtils(StringRedisTemplate redis, ObjectMapper objectMapper, long cacheTtlSeconds) {
        this(redis, objectMapper, true, cacheTtlSeconds, true, 120, 60);
    }

    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader) {
        if (!cacheEnabled)
            return loader.get();
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null)
                return objectMapper.readValue(cached, type);
            String lockKey = key + ":load-lock";
            String owner = UUID.randomUUID().toString();
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, owner, Duration.ofSeconds(10)))) {
                try {
                    T value = loader.get();
                    redis.opsForValue().set(key, objectMapper.writeValueAsString(value), cacheTtl);
                    return value;
                } finally {
                    redis.execute(RELEASE_SCRIPT, List.of(lockKey), owner);
                }
            }
            String raced = redis.opsForValue().get(key);
            return raced == null ? loader.get() : objectMapper.readValue(raced, type);
        } catch (Exception exception) {
            return loader.get();
        }
    }

    public void evictFromCache(String key) {
        if (cacheEnabled)
            redis.delete(key);
    }

    public LockLease tryAcquire(String key, Duration lease) {
        String owner = UUID.randomUUID().toString();
        String fencingToken = String.valueOf(redis.opsForValue().increment(key + ":fence"));
        boolean acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, lease));
        return acquired ? new LockLease(key, owner, fencingToken) : null;
    }

    public boolean release(LockLease lease) {
        return redis.execute(RELEASE_SCRIPT, List.of(lease.key()), lease.owner()) == 1L;
    }

    public boolean allow(String subject) {
        String key = "forge:rate-limit:" + subject;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L)
            redis.expire(key, rateLimitWindow);
        return count != null && count <= rateLimit;
    }

    public long count(String subject) {
        String value = redis.opsForValue().get("forge:rate-limit:" + subject);
        return value == null ? 0 : Long.parseLong(value);
    }

    public HandlerInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled)
            registry.addInterceptor(new RateLimitInterceptor()).addPathPatterns("/api/**");
    }

    public record LockLease(String key, String owner, String fencingToken) {
    }

    private class RateLimitInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String subject = subject(request);
            boolean allowed;
            try {
                allowed = allow(subject);
            } catch (RedisSystemException exception) {
                return true;
            }
            response.setHeader("X-RateLimit-Limit", Integer.toString(rateLimit));
            response.setHeader("X-RateLimit-Remaining", Long.toString(Math.max(0, rateLimit - count(subject))));
            if (!allowed) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(rateLimitWindow.toSeconds()));
                return false;
            }
            return true;
        }

        private String subject(HttpServletRequest request) {
            String forwarded = request.getHeader("X-Forwarded-For");
            String address = forwarded == null || forwarded.isBlank()
                    ? request.getRemoteAddr()
                    : forwarded.split(",")[0].trim();
            return address + ":" + request.getRequestURI();
        }
    }
}
