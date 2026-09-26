package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.ModuleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Summary of the learning result referenced by a submission. Fields not relevant to
 * {@link #moduleType} are null: PRONUNCIATION uses overall/fluency/completeness/stress/audio,
 * VOCABULARY (a mini-game round) uses score/correctCount/totalQuestions/gameType/duration/xp, SPEAKING uses taskCompletion/fluency/intonation/xp.
 */
@Data
@Builder
public class SubmissionResultResponse {
    private ModuleType moduleType;
    private Long resultId;
    private LocalDateTime completedAt;
    private Integer overallScore;
    private Integer fluencyScore;
    private Integer completenessScore;
    private Integer taskCompletionScore;
    private Integer intonationScore;
    private Boolean stressCorrect;
    private String studentAudioUrl;
    private Integer correctCount;
    private Integer totalQuestions;
    private String gameType;
    private Integer durationSeconds;
    private Integer xpEarned;
}
