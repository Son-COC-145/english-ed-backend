package com.example.english_app.repository.notification;

import com.example.english_app.entity.notification.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    @Query(value = "SELECT * FROM notification_outbox " +
            "WHERE (status = 'PENDING' AND available_at <= CURRENT_TIMESTAMP) " +
            "OR (status = 'PROCESSING' AND locked_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes') " +
            "ORDER BY id FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<NotificationOutboxEvent> lockDispatchableEvents(@Param("limit") int limit);
}
