package com.example.english_app.entity.ipa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity tương ứng với bảng failed_jobs — Dead Letter Queue cho XP gamification.
 * Cron Job định kỳ sẽ quét bảng này và reprocess các job có status = PENDING.
 */
@Entity
@Table(name = "failed_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 100)
    @Builder.Default
    private String jobType = "PRONUNCIATION_XP";

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Short retryCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";  // PENDING | PROCESSED | DEAD

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
