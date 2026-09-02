package com.forge.job.infrastructure.persistence;

import com.forge.job.application.port.JobRepository;
import com.forge.job.domain.Job;
import com.forge.job.domain.JobId;
import com.forge.job.domain.JobPriority;
import com.forge.job.domain.JobStatus;
import com.forge.job.domain.JobType;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("local")
public class JdbcJobRepository implements JobRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Job> findById(JobId id) {
        return queryById(id.value());
    }

    @Override
    public Optional<Job> findByIdempotencyKey(String key) {
        return queryByIdempotencyKey(key);
    }

    @Override
    public Job save(Job job) {
        int updated = jdbcTemplate.update("""
                UPDATE jobs
                SET status = ?, retry_count = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, job.getStatus().name(), job.getRetryCount(), job.getId().value());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO jobs
                    (id, type, tenant_id, payload, priority, status, retry_count, max_retries, idempotency_key)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, job.getId().value(), job.getType().name(), job.getTenantId(), job.getPayload(),
                    job.getPriority().name(), job.getStatus().name(), job.getRetryCount(), job.getMaxRetries(),
                    job.getIdempotencyKey().value());
        }
        return job;
    }

    private Optional<Job> queryById(Object parameter) {
        return query("SELECT * FROM jobs WHERE id = ?", parameter);
    }

    private Optional<Job> queryByIdempotencyKey(Object parameter) {
        return query("SELECT * FROM jobs WHERE idempotency_key = ?", parameter);
    }

    private Optional<Job> query(String sql, Object parameter) {
        return jdbcTemplate.query(sql, resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(Job.rehydrate(
                    resultSet.getObject("id", java.util.UUID.class),
                    JobType.valueOf(resultSet.getString("type")),
                    resultSet.getObject("tenant_id", java.util.UUID.class),
                    resultSet.getString("payload"),
                    JobPriority.valueOf(resultSet.getString("priority")),
                    JobStatus.valueOf(resultSet.getString("status")),
                    resultSet.getInt("retry_count"),
                    resultSet.getInt("max_retries"),
                    resultSet.getString("idempotency_key")));
        }, parameter);
    }
}