package com.forge.job.api.dto;

import com.forge.job.domain.JobPriority;
import com.forge.job.domain.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmitJobRequest {
    @NotNull
    public JobType type;
    @NotNull
    public String tenantId;
    @NotBlank
    public String payload;
    @NotNull
    public JobPriority priority;
}