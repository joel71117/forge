package com.forge.infrastructure.redis;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FencedScheduledTaskTest {
    @Test
    void staleOwnerCannotAdvanceScheduledTaskFence() {
        RedisUtils redis = mock(RedisUtils.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(redis.tryAcquire("forge:scheduled:job-maintenance", Duration.ofSeconds(30)))
                .thenReturn(new RedisUtils.LockLease("forge:scheduled:job-maintenance", "owner", "2"));

        new FencedScheduledTask(redis, jdbc, Duration.ofSeconds(30)).run();

        verify(jdbc).update(contains("last_fencing_token < ?"), eq(2L), eq("job-maintenance"), eq(2L));
        verify(redis).release(new RedisUtils.LockLease("forge:scheduled:job-maintenance", "owner", "2"));
    }
}
