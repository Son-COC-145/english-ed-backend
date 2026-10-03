package com.example.english_app.dto.response.roadmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root object đại diện cho một lộ trình học tập hoàn chỉnh.
 * Lộ trình này sẽ được serialize ra JSON và lưu vào `student_onboarding.roadmap_json`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapResponse {

    /** Version of the stored roadmap JSON contract, independent from per-user generation version. */
    @Builder.Default
    private int schemaVersion = 2;

    private String currentCefrLevel;

    private String targetCefrLevel;
    
    /** Trình độ CEFR mục tiêu của lộ trình (thường là cấp độ hiện tại của học viên) */
    private String cefrLevel;
    
    /** Tổng số tuần trong lộ trình */
    private int totalWeeks;

    /** Danh sách các mốc học tập (từng tuần) */
    private List<RoadmapMilestone> milestones;
}
