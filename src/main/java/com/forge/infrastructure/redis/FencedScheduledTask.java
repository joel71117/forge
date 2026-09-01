package com.forge.infrastructure.redis;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "forge.scheduled-ownership.enabled", havingValue = "true")
public class FencedScheduledTask {
    private static final String TASK_NAME = "job-maintenance";

    private final RedisUtils redis;
    private final JdbcTemplate jdbc;
    private final Duration lease;

    public FencedScheduledTask(RedisUtils redis, JdbcTemplate jdbc,
            @Value("${forge.scheduled-ownership.lease:30s}") Duration lease) {
        this.redis = redis;
        this.jdbc = jdbc;
        this.lease = lease;
    }

    @Scheduled(fixedDelayString = "${forge.scheduled-ownership.interval-ms:10000}")
    public void run() {
        var ownership = redis.tryAcquire("forge:scheduled:" + TASK_NAME, lease);
        if (ownership == null)
            return;
        try {
            jdbc.update("""
                    UPDATE scheduled_task_ownership
                    SET last_fencing_token = ?, last_run_at = CURRENT_TIMESTAMP
                    WHERE task_name = ? AND last_fencing_token < ?
                    """, Long.parseLong(ownership.fencingToken()), TASK_NAME,
                    Long.parseLong(ownership.fencingToken()));
        } finally {
            redis.release(ownership);
        }
    }
}
