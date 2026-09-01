package com.forge.job.api.dto;

import com.forge.job.domain.*;

public record JobResponse(String id, JobType type, String tenantId, String payload, JobPriority priority,
        JobStatus status, int retryCount) {
    public static JobResponse from(Job job) {
        return new JobResponse(job.getId().value().toString(), job.getType(), job.getTenantId().toString(),
                job.getPayload(), job.getPriority(), job.getStatus(), job.getRetryCount());
    }
}