package com.example.english_app.dto.response.roadmap;

import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class RoadmapProgressResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roadmapVersion;
    private int schemaVersion;
    private RoadmapItemStatus status;
    @JsonProperty("isCompleted")
    private boolean completed;
    private String currentCefrLevel;
    private String targetCefrLevel;
    private String cefrLevel;
    private int totalWeeks;
    private int completedWeeks;
    private Integer currentWeek;
    private int totalModules;
    private int completedModules;
    private double percentCompleted;
    private String currentModuleKey;
    /** @deprecated use currentModuleKey. Kept during the mobile migration window. */
    @Deprecated
    private String nextSuggestedModule;
    private List<RoadmapMilestone> milestones;
}
