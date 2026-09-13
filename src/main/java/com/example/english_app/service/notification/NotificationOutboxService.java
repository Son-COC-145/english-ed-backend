package com.example.english_app.service.notification;

import com.example.english_app.entity.enums.NotificationType;
import com.example.english_app.entity.enums.OutboxEventStatus;
import com.example.english_app.entity.notification.NotificationOutboxEvent;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.notification.NotificationOutboxRepository;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {
    private static final int MAX_ATTEMPTS = 10;

    private final NotificationOutboxRepository outboxRepository;
    private final UserRepository userRepository;

    @Transactional
    public void enqueue(Long recipientId, NotificationType type, String title, String body, String idempotencyKey) {
        if (outboxRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        User recipient = userRepository.getReferenceById(recipientId);
        outboxRepository.save(NotificationOutboxEvent.builder()
                .idempotencyKey(idempotencyKey)
                .recipient(recipient)
                .notificationType(type)
                .title(title)
                .body(body)
                .status(OutboxEventStatus.PENDING)
                .availableAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public List<NotificationOutboxEvent> claimBatch(int limit) {
        List<NotificationOutboxEvent> events = outboxRepository.lockDispatchableEvents(limit);
        LocalDateTime now = LocalDateTime.now();
        events.forEach(event -> {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setLockedAt(now);
            event.setClaimToken(UUID.randomUUID().toString());
        });
        return events;
    }

    @Transactional
    public void markSent(NotificationOutboxEvent event) {
        outboxRepository.completeClaim(event.getId(), event.getClaimToken(), OutboxEventStatus.SENT,
                LocalDateTime.now(), null, event.getAttemptCount(), event.getAvailableAt());
    }

    @Transactional
    public void markFailure(NotificationOutboxEvent event, Exception exception) {
        int attempts = event.getAttemptCount() + 1;
        long delaySeconds = Math.min(3600, 1L << Math.min(attempts, 10));
        outboxRepository.completeClaim(event.getId(), event.getClaimToken(),
                attempts >= MAX_ATTEMPTS ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING,
                null, exception.getClass().getSimpleName(), attempts,
                LocalDateTime.now().plusSeconds(delaySeconds));
    }
}
