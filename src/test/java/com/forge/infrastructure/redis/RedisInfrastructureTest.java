package com.forge.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class RedisInfrastructureTest {
    @Test
    void returnsCachedValueWithoutCallingLoader() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("product:1")).thenReturn("\"cached\"");

        RedisUtils redisUtils = new RedisUtils(redis, new ObjectMapper(), 60);
        String result = redisUtils.getOrLoad("product:1", String.class, () -> "loaded");

        assertEquals("cached", result);
        verify(values).get("product:1");
    }

    @Test
    void evictsCacheEntry() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisUtils redisUtils = new RedisUtils(redis, new ObjectMapper(), 60);

        redisUtils.evictFromCache("product:1");

        verify(redis).delete("product:1");
    }

    @Test
    void loadsAndCachesValueWhenCacheMisses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("product:2")).thenReturn(null);
        when(values.setIfAbsent(org.mockito.ArgumentMatchers.eq("product:2:load-lock"),
                org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(10)))).thenReturn(true);

        RedisUtils redisUtils = new RedisUtils(redis, new ObjectMapper(), 60);
        assertEquals("loaded", redisUtils.getOrLoad("product:2", String.class, () -> "loaded"));

        verify(values).set("product:2", "\"loaded\"", Duration.ofSeconds(60));
        verify(redis).execute(org.mockito.ArgumentMatchers.any(), eq(java.util.List.of("product:2:load-lock")),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void fallsBackToLoaderWhenCachedPayloadIsInvalid() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("product:3")).thenReturn("not-json");

        RedisUtils redisUtils = new RedisUtils(redis, new ObjectMapper(), 60);

        assertEquals("fresh", redisUtils.getOrLoad("product:3", String.class, () -> "fresh"));
    }

    @Test
    void rejectsNonPositiveCacheTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(IllegalArgumentException.class, () -> new RedisUtils(redis, objectMapper, 0));
    }

    @Test
    void enforcesSharedRateLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("forge:rate-limit:user-1")).thenReturn(1L, 2L, 3L);

        RedisUtils limiter = new RedisUtils(redis, new ObjectMapper(), false, 60, true, 2, 60);

        assertTrue(limiter.allow("user-1"));
        assertTrue(limiter.allow("user-1"));
        assertFalse(limiter.allow("user-1"));
        verify(redis).expire("forge:rate-limit:user-1", Duration.ofMinutes(1));
    }
}
