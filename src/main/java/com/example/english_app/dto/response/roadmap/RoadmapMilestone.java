package com.example.english_app.dto.response.roadmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Một cột mốc (milestone) trong lộ trình học tập, thường tương ứng với 1 tuần học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapMilestone {
    
    /** Thứ tự tuần học (ví dụ: 1) */
    private int weekNumber;
    
    /** Tiêu đề (ví dụ: "Làm quen với giao tiếp cơ bản") */
    private String title;
    
    /** Mô tả mục tiêu của tuần */
    private String description;
    
    /** Danh sách các module học tập trong tuần này */
    private List<RoadmapModule> modules;
}
