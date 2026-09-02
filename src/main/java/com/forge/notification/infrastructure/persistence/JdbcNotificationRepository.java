package com.forge.notification.infrastructure.persistence;

import com.forge.notification.application.port.NotificationRepository;
import com.forge.notification.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("local")
public class JdbcNotificationRepository implements NotificationRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return findByIdValue(id.value());
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String key) {
        return findByIdempotencyKeyValue(key);
    }

    @Override
    public Notification save(Notification notification) {
        int updated = jdbcTemplate.update("""
                UPDATE notifications SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, notification.getStatus().name(), notification.getId().value());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO notifications
                    (id, customer_id, type, channel, priority, status, idempotency_key)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, notification.getId().value(), notification.getCustomerId(), notification.getType(),
                    notification.getChannel().name(), notification.getPriority().name(), notification.getStatus().name(),
                    notification.getIdempotencyKey().value());
        }
        return notification;
    }

    @Override
    public NotificationAttempt saveAttempt(NotificationAttempt attempt) {
        jdbcTemplate.update("""
                INSERT INTO notification_attempts
                (id, notification_id, provider, attempt_number, started_at, finished_at, status,
                 provider_reference, error_code, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, attempt.getId().value(), attempt.getNotificationId(), attempt.getProvider(),
                attempt.getAttemptNumber(), attempt.getStartedAt(), attempt.getFinishedAt(), attempt.getStatus(),
                attempt.getProviderReference(), attempt.getErrorCode(), attempt.getErrorMessage());
        return attempt;
    }

    private Optional<Notification> findByIdValue(Object parameter) {
        return find("SELECT * FROM notifications WHERE id = ?", parameter);
    }

    private Optional<Notification> findByIdempotencyKeyValue(Object parameter) {
        return find("SELECT * FROM notifications WHERE idempotency_key = ?", parameter);
    }

    private Optional<Notification> find(String sql, Object parameter) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.of(Notification.rehydrate(rs.getObject("id", UUID.class),
                    rs.getObject("customer_id", UUID.class), rs.getString("type"),
                    NotificationChannel.valueOf(rs.getString("channel")),
                    NotificationPriority.valueOf(rs.getString("priority")), rs.getString("idempotency_key"),
                    NotificationStatus.valueOf(rs.getString("status"))));
        }, parameter);
    }
}
