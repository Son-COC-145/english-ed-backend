package com.example.english_app.entity.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_push_deliveries")
@Getter
@NoArgsConstructor
public class NotificationPushDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "outbox_event_id", nullable = false)
    private Long outboxEventId;
    @Column(name = "device_token", nullable = false, columnDefinition = "TEXT")
    private String deviceToken;
    @Column(nullable = false, length = 20)
    private String status;
}
