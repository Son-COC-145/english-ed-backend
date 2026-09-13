package com.example.english_app.service.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FcmService {
    /** Returns false only for an expired/unregistered device. Other failures are retryable. */
    public boolean sendToken(String token, String title, String body, Long eventId) {
        if (firebaseMessaging == null) {
            throw new IllegalStateException("FirebaseMessaging is not configured");
        }
        try {
            firebaseMessaging.send(Message.builder()
                    .setToken(token)
                    .putData("eventId", eventId.toString())
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build());
            return true;
        } catch (FirebaseMessagingException exception) {
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                return false;
            }
            throw new IllegalStateException("FCM delivery failed", exception);
        }
    }
    
    private final FirebaseMessaging firebaseMessaging;

    public FcmService(@Nullable @Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendMulticast(List<String> tokens, String title, String body) {
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging is not initialized. Skipping FCM message: {}", title);
            return;
        }

        if(tokens == null || tokens.isEmpty()) {
            log.debug("No tokens provided for FCM Multicast message");
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder().addAllTokens(tokens)
                        .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                        .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM Multicaset send. Success: {}, Failure: {}", response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                log.warn("Some FCM messages failed to send");
            }
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM multicast message: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to send FCM notification", e);
        } catch (Exception e) {
            log.error("Unexpected error during FCM transmission", e);
            throw new IllegalStateException("Failed to send FCM notification", e);
        }
    }
}
