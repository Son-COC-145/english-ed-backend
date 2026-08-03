package com.example.english_app.dto.response.roadmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Module học tập cụ thể trong một milestone.
 * Ví dụ: Từ vựng chủ đề Travel, Speaking tình huống đặt phòng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapModule {
    /** Loại module: "VOCABULARY", "SPEAKING", "GRAMMAR" */
    private String type;
    
    /** Tiêu đề module */
    private String title;
    
    /** ID topic liên quan (nếu có) */
    private Short topicId;
    
    /** Số lượng item trong module (số từ vựng, số câu hỏi...) */
    private int itemCount;
    
    /** Trình độ CEFR của module */
    private String cefrLevel;
}
