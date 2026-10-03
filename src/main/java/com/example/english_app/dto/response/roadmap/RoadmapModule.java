package com.example.english_app.dto.response.roadmap;

import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapModule {
    private String type;

    private String title;

    private Short topicId;

    private int itemCount;

    private String cefrLevel;

    private String moduleKey;

    private List<Long> contentItemIds;

    private String contentVersion;

    /**
     * Các trường dưới đây chỉ được enrich khi trả API, không lưu vào roadmap_json.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RoadmapItemStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("isCompleted")
    private Boolean completed;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("isAccessible")
    private Boolean accessible;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer doneCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double progressPercent;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RoadmapUnlockCondition unlockCondition;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private java.time.LocalDateTime completedAt;
}
