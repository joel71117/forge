package com.forge.infrastructure.idempotency;

import com.forge.common.api.ConflictException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@Profile("local")
public class IdempotencyRequestStore {
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRequestStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID reserve(String operation, String key, String requestHash) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO idempotency_requests (operation, idempotency_key, request_hash, status)
                VALUES (?, ?, ?, 'PROCESSING')
                ON CONFLICT (operation, idempotency_key) DO NOTHING
                """, operation, key, requestHash);
        if (inserted == 1) {
            return null;
        }
        var existing = jdbcTemplate.queryForMap("""
                SELECT request_hash, status, resource_id
                FROM idempotency_requests
                WHERE operation = ? AND idempotency_key = ?
                """, operation, key);
        if (!requestHash.equals(existing.get("request_hash"))) {
            throw new ConflictException("Idempotency-Key was already used with a different request");
        }
        if ("COMPLETED".equals(existing.get("status")) && existing.get("resource_id") != null) {
            return (UUID) existing.get("resource_id");
        }
        throw new ConflictException("An operation with this Idempotency-Key is still processing");
    }

    public void complete(String operation, String key, UUID resourceId) {
        jdbcTemplate.update("""
                UPDATE idempotency_requests
                SET status = 'COMPLETED', resource_id = ?, completed_at = CURRENT_TIMESTAMP
                WHERE operation = ? AND idempotency_key = ?
                """, resourceId, operation, key);
    }
}