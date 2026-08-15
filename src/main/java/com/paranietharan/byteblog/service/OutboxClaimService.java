package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import com.paranietharan.byteblog.entity.NotificationOutbox;
import com.paranietharan.byteblog.entity.OutboxStatus;
import com.paranietharan.byteblog.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationOutboxRepository outboxRepository;
    private final MailNotificationProperties properties;

    @Transactional
    public Optional<NotificationOutbox> claimNext() {
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id FROM notification_outbox
                WHERE status = 'PENDING' AND available_at <= ?
                ORDER BY created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                LocalDateTime.now()
        );
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        NotificationOutbox outbox = outboxRepository.findById(ids.getFirst()).orElseThrow();
        outbox.setStatus(OutboxStatus.PROCESSING);
        outbox.setLockedAt(LocalDateTime.now());
        outbox.setAttempts(outbox.getAttempts() + 1);
        return Optional.of(outboxRepository.save(outbox));
    }

    @Transactional
    public void markSent(UUID id) {
        NotificationOutbox outbox = outboxRepository.findById(id).orElseThrow();
        outbox.setStatus(OutboxStatus.SENT);
        outbox.setProcessedAt(LocalDateTime.now());
        outbox.setLockedAt(null);
        outbox.setLastError(null);
        outboxRepository.save(outbox);
    }

    @Transactional
    public void markFailed(UUID id, Exception exception) {
        NotificationOutbox outbox = outboxRepository.findById(id).orElseThrow();
        outbox.setLockedAt(null);
        outbox.setLastError(abbreviate(exception.getMessage(), 1000));
        if (outbox.getAttempts() >= outbox.getMaxAttempts()) {
            outbox.setStatus(OutboxStatus.FAILED);
        } else {
            long delaySeconds = Math.min(3600, 60L << Math.min(outbox.getAttempts() - 1, 6));
            outbox.setStatus(OutboxStatus.PENDING);
            outbox.setAvailableAt(LocalDateTime.now().plusSeconds(delaySeconds));
        }
        outboxRepository.save(outbox);
    }

    @Transactional
    public int recoverStaleClaims() {
        return jdbcTemplate.update(
                """
                UPDATE notification_outbox
                SET status = 'PENDING', locked_at = NULL, available_at = ?
                WHERE status = 'PROCESSING' AND locked_at < ?
                """,
                LocalDateTime.now(),
                LocalDateTime.now().minusMinutes(properties.getOutboxProcessingTimeoutMinutes())
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "Unknown delivery error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
