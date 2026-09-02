package com.forge.job.domain;

import com.forge.commerce.common.IdempotencyKey;

import java.util.Objects;
import java.util.UUID;

/**
 * Job is a state machine.
 *
 * <p>We keep retries explicit because a job may fail temporarily, be retried, or end in a
 * dead-letter state. That is a core business concept, not a generic integer property.</p>
 */
public class Job {
    private JobId id;
    private final JobType type;
    private final UUID tenantId;
    private final String payload;
    private final JobPriority priority;
    private JobStatus status;
    private int retryCount;
    private final int maxRetries;
    private final IdempotencyKey idempotencyKey;

    public Job(JobType type, UUID tenantId, String payload, JobPriority priority, IdempotencyKey idempotencyKey) {
        this(type, tenantId, payload, priority, 3, idempotencyKey);
    }

    @SuppressWarnings("java:S107")
    public Job(JobType type, UUID tenantId, String payload, JobPriority priority, int maxRetries, IdempotencyKey idempotencyKey) {
        if (type == null) {
            throw new IllegalArgumentException("Job type cannot be null.");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be blank.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null.");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key cannot be null.");
        }

        this.id = new JobId(UUID.randomUUID());
        this.type = type;
        this.tenantId = tenantId;
        this.payload = payload;
        this.priority = priority;
        this.status = JobStatus.QUEUED;
        this.retryCount = 0;
        this.maxRetries = maxRetries;
        this.idempotencyKey = idempotencyKey;
    }

    @SuppressWarnings("java:S107")
    public static Job rehydrate(UUID id, JobType type, UUID tenantId, String payload, JobPriority priority,
            JobStatus status, int retryCount, int maxRetries, String idempotencyKey) {
        var job = new Job(type, tenantId, payload, priority, maxRetries, new IdempotencyKey(idempotencyKey));
        job.id = new JobId(id);
        job.status = status;
        job.retryCount = retryCount;
        return job;
    }

    public JobId getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getPayload() {
        return payload;
    }

    public JobPriority getPriority() {
        return priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }

    public void start() {
        if (status != JobStatus.QUEUED && status != JobStatus.RETRYING) {
            throw new IllegalStateException("Only QUEUED or RETRYING jobs can start.");
        }
        this.status = JobStatus.RUNNING;
    }

    public void complete() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException("Only RUNNING jobs can complete.");
        }
        this.status = JobStatus.COMPLETED;
    }

    public void fail() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException("Only RUNNING jobs can fail.");
        }
        this.status = JobStatus.FAILED;
    }

    public void retry() {
        if (status != JobStatus.FAILED) {
            throw new IllegalStateException("Only FAILED jobs can retry.");
        }
        if (retryCount >= maxRetries) {
            this.status = JobStatus.DEAD_LETTERED;
            return;
        }
        this.retryCount++;
        this.status = JobStatus.RETRYING;
    }

    public void cancel() {
        if (status == JobStatus.COMPLETED || status == JobStatus.CANCELLED) {
            throw new IllegalStateException("This job cannot be cancelled in its current state.");
        }
        this.status = JobStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job job)) return false;
        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
