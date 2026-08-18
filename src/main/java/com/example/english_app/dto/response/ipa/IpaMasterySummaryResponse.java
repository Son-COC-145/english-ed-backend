package com.example.english_app.dto.response.ipa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpaMasterySummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalPhonemes;
    private int masteredCount;
    private int learningCount;
    private int needsPracticeCount;
    private int unlearnedCount;
    private double overallMasteryPercent;
    private List<IpaPhonemeMasteryDto> phonemes;
}
