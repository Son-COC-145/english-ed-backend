package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_onboarding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "goal_survey_json", columnDefinition = "jsonb")
    private String goalSurveyJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_cefr_level")
    private CefrLevel placementCefrLevel;

    @Column(name = "placement_vocab_score")
    private Short placementVocabScore;

    @Column(name = "placement_grammar_score")
    private Short placementGrammarScore;

    @Column(name = "placement_reading_score")
    private Short placementReadingScore;

    @Column(name = "placement_listening_score")
    private Short placementListeningScore;

    @Column(name = "placement_pronunciation_score")
    private Short placementPronunciationScore;

    @Column(name = "placement_completed_at")
    private LocalDateTime placementCompletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roadmap_json", columnDefinition = "jsonb")
    private String roadmapJson;

    @Column(name = "daily_goal_xp", nullable = false)
    @Builder.Default
    private Short dailyGoalXp = 20;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;
}
