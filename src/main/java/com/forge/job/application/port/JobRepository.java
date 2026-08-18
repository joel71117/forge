package com.forge.job.application.port;

import com.forge.job.domain.Job;
import com.forge.job.domain.JobId;

import java.util.Optional;

public interface JobRepository {
    Optional<Job> findById(JobId id);

    Optional<Job> findByIdempotencyKey(String key);

    Job save(Job job);
}