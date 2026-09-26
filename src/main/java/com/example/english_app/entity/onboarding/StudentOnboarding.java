package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_vocab_cefr")
    private CefrLevel placementVocabCefr;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_grammar_cefr")
    private CefrLevel placementGrammarCefr;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_reading_cefr")
    private CefrLevel placementReadingCefr;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_listening_cefr")
    private CefrLevel placementListeningCefr;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_pronunciation_cefr")
    private CefrLevel placementPronunciationCefr;

    @Column(name = "placement_completed_at")
    private LocalDateTime placementCompletedAt;

    @Column(name = "is_placement_skipped", nullable = false)
    @Builder.Default
    private Boolean isPlacementSkipped = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roadmap_json", columnDefinition = "jsonb")
    private String roadmapJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_status", length = 20)
    private RoadmapGenerationStatus roadmapStatus;

    @Column(name = "roadmap_generation_version", nullable = false)
    @Builder.Default
    private Integer roadmapGenerationVersion = 0;

    @Column(name = "roadmap_generation_attempts", nullable = false)
    @Builder.Default
    private Integer roadmapGenerationAttempts = 0;

    @Column(name = "roadmap_last_error", length = 200)
    private String roadmapLastError;

    @Column(name = "roadmap_updated_at")
    private LocalDateTime roadmapUpdatedAt;

    @Column(name = "daily_goal_xp")
    private Short dailyGoalXp;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;
}
