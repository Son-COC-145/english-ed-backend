package com.example.english_app.repository.notification;

import com.example.english_app.entity.notification.NotificationPushDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationPushDeliveryRepository extends JpaRepository<NotificationPushDelivery, Long> {
        @Modifying
        @Transactional
        @Query(value = "INSERT INTO notification_push_deliveries (outbox_event_id,device_token) " +
                        "VALUES (:eventId,:token) ON CONFLICT (outbox_event_id,device_token) DO NOTHING", nativeQuery = true)
        void createDelivery(@Param("eventId") Long eventId, @Param("token") String token);

        @Transactional
        default void createDeliveries(Long eventId, List<String> tokens) {
                for (String token : tokens)
                        createDelivery(eventId, token);
        }

        @Query(value = "SELECT device_token FROM notification_push_deliveries " +
                        "WHERE outbox_event_id=:eventId AND status='PENDING'", nativeQuery = true)
        List<String> findPendingTokens(@Param("eventId") Long eventId);

        @Query(value = "SELECT EXISTS(SELECT 1 FROM notification_outbox " +
                        "WHERE id=:eventId AND claim_token=:claimToken AND status='PROCESSING')", nativeQuery = true)
        boolean isCurrentClaim(@Param("eventId") Long eventId, @Param("claimToken") String claimToken);

        @Modifying
        @Transactional
        @Query(value = "UPDATE notification_push_deliveries SET status=CASE WHEN :valid THEN 'SENT' ELSE 'INVALID' END,"
                        +
                        "sent_at=NOW(),last_error=NULL WHERE outbox_event_id=:eventId AND device_token=:token " +
                        "AND status='PENDING' AND EXISTS(SELECT 1 FROM notification_outbox o WHERE o.id=:eventId " +
                        "AND o.claim_token=:claimToken AND o.status='PROCESSING')", nativeQuery = true)
        int markDelivered(@Param("eventId") Long eventId, @Param("token") String token, @Param("valid") boolean valid,
                        @Param("claimToken") String claimToken);

        @Modifying
        @Transactional
        @Query(value = "UPDATE notification_push_deliveries SET last_error=:error " +
                        "WHERE outbox_event_id=:eventId AND device_token=:token AND status='PENDING' " +
                        "AND EXISTS(SELECT 1 FROM notification_outbox o WHERE o.id=:eventId " +
                        "AND o.claim_token=:claimToken AND o.status='PROCESSING')", nativeQuery = true)
        void markFailure(@Param("eventId") Long eventId, @Param("token") String token, @Param("error") String error,
                        @Param("claimToken") String claimToken);

        @Modifying
        @Transactional
        @Query(value = "DELETE FROM user_device_tokens WHERE token=:token", nativeQuery = true)
        void removeInvalidToken(@Param("token") String token);
}
