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
        });
        return events;
    }

    @Transactional
    public void markSent(Long eventId) {
        NotificationOutboxEvent event = outboxRepository.findById(eventId).orElseThrow();
        event.setStatus(OutboxEventStatus.SENT);
        event.setSentAt(LocalDateTime.now());
        event.setLockedAt(null);
        event.setLastError(null);
    }

    @Transactional
    public void markFailure(Long eventId, Exception exception) {
        NotificationOutboxEvent event = outboxRepository.findById(eventId).orElseThrow();
        int attempts = event.getAttemptCount() + 1;
        event.setAttemptCount(attempts);
        event.setLockedAt(null);
        event.setLastError(exception.getMessage());
        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(OutboxEventStatus.FAILED);
            return;
        }
        long delaySeconds = Math.min(3600, 1L << Math.min(attempts, 10));
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAvailableAt(LocalDateTime.now().plusSeconds(delaySeconds));
    }
}
