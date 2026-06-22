package com.example.english_app.entity.gamification;

import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentStat {

    @Id
    private Long studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Short currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Short longestStreak = 0;

    @Column(name = "streak_freeze_count", nullable = false)
    @Builder.Default
    private Short streakFreezeCount = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_study_minutes", nullable = false)
    @Builder.Default
    private Integer totalStudyMinutes = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
