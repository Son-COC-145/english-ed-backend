package com.example.english_app.entity.gamification;

import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_goals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "goal_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "goal_date", nullable = false)
    private LocalDate goalDate;

    @Column(name = "target_xp", nullable = false)
    private Short targetXp;

    @Column(name = "earned_xp", nullable = false)
    @Builder.Default
    private Short earnedXp = 0;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;
}
