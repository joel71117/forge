package com.forge.job.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.job.application.port.JobRepository;
import com.forge.job.domain.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository repository;
    private final EventPublisher eventPublisher;

    public JobService(JobRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public synchronized Job submit(String key, com.forge.job.api.dto.SubmitJobRequest request) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Idempotency-Key is required");
        var existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent())
            return existing.get();
        var job = new Job(request.type, UUID.fromString(request.tenantId), request.payload, request.priority,
                new IdempotencyKey(key));
        repository.save(job);
        eventPublisher
                .publish(new EventEnvelope("JobSubmitted", job.getId().toString(), "Job", job.getId().toString()));
        return job;
    }

    public Job get(UUID id) {
        return repository.findById(new JobId(id)).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    public Job cancel(UUID id) {
        var job = get(id);
        try {
            job.cancel();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        repository.save(job);
        return job;
    }
}