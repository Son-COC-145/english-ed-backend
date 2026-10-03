package com.example.english_app.dto.response.roadmap;

import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Một cột mốc (milestone) trong lộ trình học tập, thường tương ứng với 1 tuần
 * học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapMilestone {

    private int weekNumber;

    private String title;

    private String description;

    private List<RoadmapModule> modules;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RoadmapItemStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("isCompleted")
    private Boolean completed;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("isAccessible")
    private Boolean accessible;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double progressPercent;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RoadmapUnlockCondition unlockCondition;
}
