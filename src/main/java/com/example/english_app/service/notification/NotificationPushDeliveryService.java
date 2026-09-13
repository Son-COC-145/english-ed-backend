package com.example.english_app.service.notification;

import com.example.english_app.entity.notification.NotificationOutboxEvent;
import lombok.RequiredArgsConstructor;
import com.example.english_app.repository.notification.NotificationPushDeliveryRepository;
import org.springframework.stereotype.Service;

/** Runs after notification persistence commits; each successful token is recorded separately. */
@Service
@RequiredArgsConstructor
public class NotificationPushDeliveryService {
    private final NotificationPushDeliveryRepository pushDeliveryRepository;
    private final FcmService fcmService;

    public void deliver(NotificationOutboxEvent event) {
        var tokens = pushDeliveryRepository.findPendingTokens(event.getId());
        RuntimeException failure = null;
        for (String token : tokens) {
            if (!pushDeliveryRepository.isCurrentClaim(event.getId(), event.getClaimToken())) {
                throw new IllegalStateException("Outbox lease has been reclaimed");
            }
            try {
                boolean valid = fcmService.sendToken(token, event.getTitle(), event.getBody(), event.getId());
                pushDeliveryRepository.markDelivered(event.getId(), token, valid);
                if (!valid) pushDeliveryRepository.removeInvalidToken(token);
            } catch (RuntimeException exception) {
                pushDeliveryRepository.markFailure(event.getId(), token, exception.getClass().getSimpleName());
                failure = exception;
            }
        }
        if (failure != null) throw failure;
    }
}
