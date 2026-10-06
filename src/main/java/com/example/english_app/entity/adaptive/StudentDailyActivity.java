package com.example.english_app.entity.adaptive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_daily_activity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_daily_activity",
                columnNames = {"student_id", "activity_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Builder.Default
    @Column(name = "measured_seconds", nullable = false)
    private Integer measuredSeconds = 0;

    @Builder.Default
    @Column(name = "estimated_seconds", nullable = false)
    private Integer estimatedSeconds = 0;

    @Builder.Default
    @Column(name = "activity_count", nullable = false)
    private Integer activityCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
