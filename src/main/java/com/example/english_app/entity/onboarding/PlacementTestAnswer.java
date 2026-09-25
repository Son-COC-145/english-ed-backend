package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.question.Question;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "placement_test_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementTestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PlacementTestSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // Assume Question entity will be created

    @Column(name = "answer_given", length = 500)
    private String answerGiven;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_spent_ms")
    private Integer timeSpentMs;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "submission_type", length = 20)
    private String submissionType;

    // Derived pronunciation metrics only. Raw learner audio is intentionally never persisted.
    @Column(name = "pronunciation_overall_score")
    private Short pronunciationOverallScore;

    @Column(name = "pronunciation_accuracy_score")
    private Short pronunciationAccuracyScore;

    @Column(name = "pronunciation_fluency_score")
    private Short pronunciationFluencyScore;

    @Column(name = "pronunciation_completeness_score")
    private Short pronunciationCompletenessScore;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
