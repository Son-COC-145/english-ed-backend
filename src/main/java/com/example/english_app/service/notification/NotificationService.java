package com.example.english_app.service.notification;

import com.example.english_app.dto.request.DeviceTokenRequest;
import com.example.english_app.dto.response.NotificationResponse;
import com.example.english_app.entity.enums.NotificationType;
import com.example.english_app.entity.notification.Notification;
import com.example.english_app.entity.notification.NotificationOutboxEvent;
import com.example.english_app.entity.notification.UserDeviceToken;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.notification.NotificationRepository;
import com.example.english_app.repository.notification.UserDeviceTokenRepository;
import com.example.english_app.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.english_app.dto.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final NotificationOutboxService notificationOutboxService;

    @Transactional
    public void registerDeviceToken(DeviceTokenRequest request) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.warn("Attempt to register device token without authentication");
            return;
        }

        userDeviceTokenRepository.findByUserIdAndToken(currentUserId, request.getToken())
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(currentUserId);
                    UserDeviceToken token = UserDeviceToken.builder()
                        .user(user)
                        .token(request.getToken())
                        .deviceType(request.getDeviceType())
                        .build();
                    return userDeviceTokenRepository.save(token);
                });
        log.info("Device token registered for user: {}", currentUserId);        
    }

    @Transactional
    public void removeDeviceToken(String token) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) return;
        userDeviceTokenRepository.findByUserIdAndToken(currentUserId, token)
                .ifPresent(userDeviceTokenRepository::delete);
        log.info("Device token removed for user: {}", currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<NotificationResponse> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = getCurrentUserId();
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void sendToUser(Long userId, String title, String message, String type) {
        notificationOutboxService.enqueue(userId, NotificationType.valueOf(type), title, message,
                "notification-api:" + UUID.randomUUID());
        return;
        /*
        User user = userRepository.getReferenceById(userId);
        
        // 1. Lưu bản ghi vào DB
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(message)
                .isRead(false)
                .type(NotificationType.valueOf(type)) 
                .build();
        notificationRepository.save(notification);
        
        // 2. Lấy danh sách Token và Gửi Push Notification qua FCM
        List<String> tokens = userDeviceTokenRepository.findByUserId(userId)
                .stream()
                .map(UserDeviceToken::getToken)
                .collect(Collectors.toList());
                
        fcmService.sendMulticast(tokens, title, message);
        */
    }

    @Transactional
    public void sendFromOutbox(NotificationOutboxEvent event) {
        if (notificationRepository.existsByOutboxEventId(event.getId())) {
            return;
        }
        User user = userRepository.getReferenceById(event.getRecipient().getId());
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(event.getTitle())
                .body(event.getBody())
                .isRead(false)
                .type(event.getNotificationType())
                .outboxEventId(event.getId())
                .build());
        List<String> tokens = userDeviceTokenRepository.findByUserId(user.getId())
                .stream()
                .map(UserDeviceToken::getToken)
                .collect(Collectors.toList());
        fcmService.sendMulticast(tokens, event.getTitle(), event.getBody());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

}
