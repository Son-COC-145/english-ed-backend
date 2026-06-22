package com.example.english_app.entity.ipa;

import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "pronunciation_practice_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PronunciationPracticeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_type", nullable = false)
    private PracticeType practiceType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "overall_score", nullable = false)
    private Short overallScore;

    @Column(name = "fluency_score")
    private Short fluencyScore;

    @Column(name = "completeness_score")
    private Short completenessScore;

    @Column(name = "stress_correct")
    private Boolean stressCorrect;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phoneme_detail_json", columnDefinition = "jsonb")
    private String phonemeDetailJson;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @CreationTimestamp
    @Column(name = "practiced_at", nullable = false, updatable = false)
    private LocalDateTime practicedAt;
}
