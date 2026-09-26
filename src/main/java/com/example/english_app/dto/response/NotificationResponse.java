package com.example.english_app.dto.response;

import com.example.english_app.entity.notification.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String type;
    /** Optional in-app link the notification points to. */
    private String refUrl;

    public static NotificationResponse fromEntity(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getBody())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .refUrl(entity.getRefUrl())
                .build();
    }
}
