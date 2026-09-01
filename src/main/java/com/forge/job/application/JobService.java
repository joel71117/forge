package com.forge.job.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.job.application.port.JobRepository;
import com.forge.job.domain.*;
import com.forge.infrastructure.idempotency.IdempotencyRequestStore;
import com.forge.infrastructure.idempotency.RequestFingerprint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository repository;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<IdempotencyRequestStore> idempotencyStore;

    public JobService(JobRepository repository, EventPublisher eventPublisher,
            ObjectProvider<IdempotencyRequestStore> idempotencyStore) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.idempotencyStore = idempotencyStore;
    }

    @Transactional
    public Job submit(String key, com.forge.job.api.dto.SubmitJobRequest request) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Idempotency-Key is required");
        var requestHash = RequestFingerprint.sha256(request.type + "|" + request.tenantId + "|" + request.payload
                + "|" + request.priority);
        var store = idempotencyStore.getIfAvailable();
        if (store != null) {
            var completedId = store.reserve("JOB_SUBMIT", key, requestHash);
            if (completedId != null) {
                return get(completedId);
            }
        }
        var existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent())
            return existing.get();
        var job = new Job(request.type, UUID.fromString(request.tenantId), request.payload, request.priority,
                new IdempotencyKey(key));
        repository.save(job);
        eventPublisher
                .publish(new EventEnvelope("JobSubmitted", job.getId().toString(), "Job", job.getId().toString()));
        if (store != null) {
            store.complete("JOB_SUBMIT", key, job.getId().value());
        }
        return job;
    }

    public Job get(UUID id) {
        return repository.findById(new JobId(id)).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    @Transactional
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