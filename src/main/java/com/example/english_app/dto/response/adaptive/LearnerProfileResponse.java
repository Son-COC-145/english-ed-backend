package com.example.english_app.dto.response.adaptive;

import com.example.english_app.entity.enums.CefrLevel;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class LearnerProfileResponse {
    CefrLevel cefr;
    String cefrSource;
    LocalDateTime cefrAssessedAt;
    BigDecimal overallMastery;
    long profileVersion;
    int totalActivities;
    List<LearnerSkillResponse> skills;
}
