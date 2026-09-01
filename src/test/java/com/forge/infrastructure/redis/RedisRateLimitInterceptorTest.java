package com.forge.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class RedisRateLimitInterceptorTest {
    @Test
    void rejectsRequestAndReportsRemainingQuota() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.increment("forge:rate-limit:203.0.113.4:/checkout")).thenReturn(3L);
        when(values.get("forge:rate-limit:203.0.113.4:/checkout")).thenReturn("3");
        RedisUtils redis = new RedisUtils(redisTemplate, new ObjectMapper(), false, 60, true, 2, 30);
        HttpServletRequest request = request("203.0.113.4", "/checkout", null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        var interceptor = redis.rateLimitInterceptor();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        verify(response).setStatus(429);
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(response).setHeader("Retry-After", "30");
    }

    @Test
    void usesFirstForwardedAddressAndAllowsRequest() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.increment("forge:rate-limit:198.51.100.8:/products")).thenReturn(1L);
        when(values.get("forge:rate-limit:198.51.100.8:/products")).thenReturn("1");
        RedisUtils redis = new RedisUtils(redisTemplate, new ObjectMapper(), false, 60, true, 5, 60);
        HttpServletRequest request = request("10.0.0.2", "/products", "198.51.100.8, 10.0.0.2");
        HttpServletResponse response = mock(HttpServletResponse.class);
        var interceptor = redis.rateLimitInterceptor();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(response).setHeader("X-RateLimit-Limit", "5");
        verify(response).setHeader("X-RateLimit-Remaining", "4");
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.increment("forge:rate-limit:127.0.0.1:/health"))
                .thenThrow(mock(RedisSystemException.class));
        RedisUtils redis = new RedisUtils(redisTemplate, new ObjectMapper(), false, 60, true, 2, 60);
        HttpServletRequest request = request("127.0.0.1", "/health", null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertTrue(redis.rateLimitInterceptor().preHandle(request, response, new Object()));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsInvalidSettings() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        assertThrows(IllegalArgumentException.class,
                () -> new RedisUtils(redisTemplate, new ObjectMapper(), true, 60, true, 0, 60));
        assertThrows(IllegalArgumentException.class,
                () -> new RedisUtils(redisTemplate, new ObjectMapper(), true, 60, true, 1, 0));
    }

    private static HttpServletRequest request(String remoteAddress, String uri, String forwarded) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwarded);
        return request;
    }
}