package com.forge.job.application.handler;

import com.forge.job.domain.Job;
import com.forge.job.domain.JobType;

public interface JobHandler {
    JobType supportedType();

    void handle(Job job) throws Exception;
}