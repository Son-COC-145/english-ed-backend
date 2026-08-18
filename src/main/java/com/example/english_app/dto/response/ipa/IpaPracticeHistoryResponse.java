package com.example.english_app.dto.response.ipa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpaPracticeHistoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String word;
    private String ipaTranscription;
    private Short overallScore;
    private Short fluencyScore;
    private Short completenessScore;
    private Boolean stressCorrect;
    private String scoreColor;
    private LocalDateTime practicedAt;
}
