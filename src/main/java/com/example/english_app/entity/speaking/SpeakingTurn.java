package com.example.english_app.entity.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SpeakingSession session;

    @Column(name = "turn_index", nullable = false)
    private Short turnIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeakerRole speaker;

    @Column(name = "transcript_text", nullable = false, columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grammar_errors_json", columnDefinition = "jsonb")
    private String grammarErrorsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vocabulary_suggestions_json", columnDefinition = "jsonb")
    private String vocabularySuggestionsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
