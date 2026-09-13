package com.example.english_app.scheduler;

import com.example.english_app.entity.notification.NotificationOutboxEvent;
import com.example.english_app.service.notification.NotificationOutboxService;
import com.example.english_app.service.notification.NotificationService;
import com.example.english_app.service.notification.NotificationPushDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxScheduler {
    private final NotificationOutboxService outboxService;
    private final NotificationService notificationService;
    private final NotificationPushDeliveryService pushDeliveryService;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-ms:5000}")
    public void dispatchPendingNotifications() {
        for (NotificationOutboxEvent event : outboxService.claimBatch(50)) {
            try {
                notificationService.sendFromOutbox(event);
                pushDeliveryService.deliver(event);
                outboxService.markSent(event);
            } catch (Exception exception) {
                log.warn("Notification outbox delivery failed: eventId={}", event.getId(), exception);
                outboxService.markFailure(event, exception);
            }
        }
    }
}
