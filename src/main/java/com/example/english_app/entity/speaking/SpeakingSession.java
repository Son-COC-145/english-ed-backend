package com.example.english_app.entity.speaking;

import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SpeakingScenario scenario;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "hint_used_count", nullable = false)
    @Builder.Default
    private Short hintUsedCount = 0;

    @Column(name = "task_completion_score")
    private Short taskCompletionScore;

    @Column(name = "fluency_score")
    private Short fluencyScore;

    @Column(name = "intonation_score")
    private Short intonationScore;

    @Column(name = "xp_earned")
    private Short xpEarned;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_json", columnDefinition = "jsonb")
    private String evaluationJson;
}
