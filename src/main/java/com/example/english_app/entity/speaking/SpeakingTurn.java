package com.example.english_app.entity.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private Integer turnIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeakerRole speaker;

    @Column(name = "request_key", length = 100)
    private String requestKey;

    @Column(name = "input_hash", length = 64)
    private String inputHash;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "audio_data", columnDefinition = "bytea")
    private byte[] audioData;

    @Column(name = "audio_content_type", length = 100)
    private String audioContentType;

    @Column(name = "transcript_text", nullable = false, columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audio_metrics_json", columnDefinition = "jsonb")
    private String audioMetricsJson;

    @Column(name = "evaluation_status", nullable = false, length = 32)
    @Builder.Default
    private String evaluationStatus = "PENDING";

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "response_text_ready", nullable = false)
    private boolean responseTextReady;

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
