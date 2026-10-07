package com.example.english_app.dto.response.adaptive;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerEvidenceSource;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.MasteryTrend;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class LearnerSkillResponse {
    LearnerSkill skill;
    CefrLevel cefrLevel;
    LearnerEvidenceSource source;
    BigDecimal mastery;
    BigDecimal recentScore;
    BigDecimal confidence;
    MasteryTrend trend;
    int evidenceCount;
    LocalDateTime lastPracticedAt;
}
