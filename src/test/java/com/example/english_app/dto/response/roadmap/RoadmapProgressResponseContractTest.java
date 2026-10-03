package com.example.english_app.dto.response.roadmap;

import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoadmapProgressResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void serializesStableWeeklyMobileContract() throws Exception {
        RoadmapModule module = RoadmapModule.builder()
                .moduleKey("VOCABULARY:12")
                .type("VOCABULARY")
                .status(RoadmapItemStatus.IN_PROGRESS)
                .completed(false)
                .accessible(true)
                .doneCount(5)
                .totalCount(10)
                .progressPercent(50.0)
                .build();
        RoadmapMilestone week = RoadmapMilestone.builder()
                .weekNumber(2)
                .status(RoadmapItemStatus.IN_PROGRESS)
                .completed(false)
                .accessible(true)
                .progressPercent(50.0)
                .modules(List.of(module))
                .build();
        RoadmapProgressResponse response = RoadmapProgressResponse.builder()
                .schemaVersion(2)
                .roadmapVersion(3)
                .status(RoadmapItemStatus.IN_PROGRESS)
                .completed(false)
                .currentCefrLevel("A2")
                .targetCefrLevel("B1")
                .currentWeek(2)
                .currentModuleKey(module.getModuleKey())
                .milestones(List.of(week))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(json.path("schemaVersion").asInt()).isEqualTo(2);
        assertThat(json.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(json.path("isCompleted").asBoolean()).isFalse();
        assertThat(json.path("currentModuleKey").asText()).isEqualTo(module.getModuleKey());
        assertThat(json.has("currentDayKey")).isFalse();
        JsonNode weekJson = json.path("milestones").get(0);
        assertThat(weekJson.has("days")).isFalse();
        assertThat(weekJson.path("isAccessible").asBoolean()).isTrue();
        JsonNode moduleJson = weekJson.path("modules").get(0);
        assertThat(moduleJson.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(moduleJson.path("isCompleted").asBoolean()).isFalse();
        assertThat(moduleJson.path("isAccessible").asBoolean()).isTrue();
        assertThat(moduleJson.has("completed")).isFalse();
        assertThat(moduleJson.has("accessible")).isFalse();
    }
}
