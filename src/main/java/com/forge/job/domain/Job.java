package com.forge.job.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Job {
    private final JobId id;
    private String type;
    private final UUID tenantId;
    private Map<String, Object> payload;
    private int priority;
    private JobStatus status;
    private Instant scheduledAt;
    private int retryCount;
    private int maxRetries;
    private Instant nextAttemptAt;
    private Instant leaseUntil;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;

    public Job(String type, UUID tenantId, Map<String, Object> payload, int priority, JobStatus status,
               Instant scheduledAt, int retryCount, int maxRetries, Instant nextAttemptAt,
               Instant leaseUntil, Instant createdAt, Instant startedAt, Instant completedAt,
               Instant updatedAt) {
        this.id = new JobId(UUID.randomUUID());
        this.type = type;
        this.tenantId = tenantId;
        this.payload = payload;
        this.priority = priority;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.nextAttemptAt = nextAttemptAt;
        this.leaseUntil = leaseUntil;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }
}
