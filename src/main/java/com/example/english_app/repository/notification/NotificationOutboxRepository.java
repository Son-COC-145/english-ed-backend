package com.example.english_app.repository.notification;

import com.example.english_app.entity.notification.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import com.example.english_app.entity.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {
        boolean existsByIdempotencyKey(String idempotencyKey);

        @Modifying
        @Query("update NotificationOutboxEvent e set e.status = :status, e.lockedAt = null, " +
                        "e.claimToken = null, e.sentAt = :sentAt, e.lastError = :error, " +
                        "e.attemptCount = :attempts, e.availableAt = :availableAt " +
                        "where e.id = :id and e.status = 'PROCESSING' and e.claimToken = :claimToken")
        int completeClaim(@Param("id") Long id, @Param("claimToken") String claimToken,
                        @Param("status") OutboxEventStatus status,
                        @Param("sentAt") LocalDateTime sentAt, @Param("error") String error,
                        @Param("attempts") int attempts, @Param("availableAt") LocalDateTime availableAt);

        @Query(value = "SELECT * FROM notification_outbox " +
                        "WHERE (status = 'PENDING' AND available_at <= CURRENT_TIMESTAMP) " +
                        "OR (status = 'PROCESSING' AND locked_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes') " +
                        "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
        List<NotificationOutboxEvent> lockDispatchableEvents(@Param("limit") int limit);
}
