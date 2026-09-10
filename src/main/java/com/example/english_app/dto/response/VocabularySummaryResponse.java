package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tóm tắt tiến trình từ vựng cho màn Home.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularySummaryResponse {
    private long total;
    private long newCount;
    private long learningCount;
    private long reviewingCount;
    private long masteredCount;
    private long dueTodayCount;
}
