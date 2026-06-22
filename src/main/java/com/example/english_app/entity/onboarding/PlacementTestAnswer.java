package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.question.Question;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
