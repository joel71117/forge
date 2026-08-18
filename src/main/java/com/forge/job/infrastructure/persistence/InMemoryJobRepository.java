package com.forge.job.infrastructure.persistence;

import com.forge.job.application.port.JobRepository;
import com.forge.job.domain.Job;
import com.forge.job.domain.JobId;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryJobRepository implements JobRepository {
    private final ConcurrentHashMap<JobId, Job> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Job> findById(JobId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Job> findByIdempotencyKey(String key) {
        return store.values().stream().filter(j -> j.getIdempotencyKey().value().equals(key)).findFirst();
    }

    @Override
    public Job save(Job job) {
        store.put(job.getId(), job);
        return job;
    }
}