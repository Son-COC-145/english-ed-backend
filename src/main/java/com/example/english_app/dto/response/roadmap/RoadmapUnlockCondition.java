package com.example.english_app.dto.response.roadmap;

import com.example.english_app.entity.enums.RoadmapUnlockReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapUnlockCondition {
    private RoadmapUnlockReason reasonCode;
    private Integer prerequisiteWeek;
}
