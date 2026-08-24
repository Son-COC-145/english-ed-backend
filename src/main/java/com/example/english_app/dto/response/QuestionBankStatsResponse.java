package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Thống kê ngân hàng câu hỏi Placement Test dành cho Admin.
 * Breakdown theo CEFR level × Skill.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankStatsResponse {

    /** Tổng số câu hỏi đang active trong toàn bộ ngân hàng đề. */
    private long totalActive;

    /** Tổng số câu hỏi đã inactive (đã tắt). */
    private long totalInactive;

    /** Thống kê số câu active theo từng CEFR Level. */
    private Map<CefrLevel, Long> byLevel;

    /** Thống kê số câu active theo từng Skill. */
    private Map<Skill, Long> bySkill;

    /** Chi tiết breakdown theo từng cặp (level, skill). */
    private List<CellStat> breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CellStat {
        private CefrLevel cefrLevel;
        private Skill skill;
        private long activeCount;
        private long inactiveCount;
    }
}
