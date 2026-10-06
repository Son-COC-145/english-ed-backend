package com.example.english_app.dto.response.adaptive;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TodayPlanResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void serializesServerOwnedCompletionAndNavigationContract() throws Exception {
        TodayPlanItemResponse activity = TodayPlanItemResponse.builder()
                .recommendationId("abc")
                .type(TodayPlanItemType.PRONUNCIATION)
                .skill(LearnerSkill.PRONUNCIATION)
                .estimatedMinutes(3)
                .target(objectMapper.createObjectNode().put("phonemeId", 7))
                .navigation(objectMapper.createObjectNode().put("route", "IPA_PHONEME"))
                .reasonCode(RecommendationReasonCode.WEAK_PHONEME)
                .reasonParams(objectMapper.createObjectNode())
                .status(TodayPlanItemStatus.IN_PROGRESS)
                .completedUnits(1)
                .totalUnits(3)
                .progressPercent(new BigDecimal("33.33"))
                .completed(false)
                .build();
        TodayPlanResponse response = TodayPlanResponse.builder()
                .date(LocalDate.of(2026, 10, 6))
                .timezone("Asia/Ho_Chi_Minh")
                .revision(2)
                .profileVersion(4)
                .rulesVersion("2026-10-06.1")
                .budgetMinutes(15)
                .estimatedMinutes(3)
                .completedMinutes(1)
                .progressPercent(new BigDecimal("33.33"))
                .completed(false)
                .nextRecommendationId("abc")
                .activities(List.of(activity))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(json.path("isCompleted").asBoolean()).isFalse();
        assertThat(json.has("completed")).isFalse();
        assertThat(json.path("nextRecommendationId").asText()).isEqualTo("abc");
        JsonNode item = json.path("activities").get(0);
        assertThat(item.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(item.path("progressPercent").decimalValue()).isEqualByComparingTo("33.33");
        assertThat(item.path("isCompleted").asBoolean()).isFalse();
        assertThat(item.has("completed")).isFalse();
        assertThat(item.path("navigation").path("route").asText()).isEqualTo("IPA_PHONEME");
    }
}
