package com.forge.job.api.dto;

import com.forge.job.domain.*;

public record JobResponse(String id, JobType type, String tenantId, String payload, JobPriority priority,
                JobStatus status, int retryCount) {
}