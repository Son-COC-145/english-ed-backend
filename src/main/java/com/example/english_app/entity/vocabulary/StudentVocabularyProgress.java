package com.example.english_app.entity.vocabulary;

import com.example.english_app.entity.enums.LearningStatus;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_vocabulary_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "vocabulary_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentVocabularyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LearningStatus status = LearningStatus.NEW;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Short correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    @Builder.Default
    private Short incorrectCount = 0;

    @Column(name = "last_practiced_at")
    private LocalDateTime lastPracticedAt;
}
