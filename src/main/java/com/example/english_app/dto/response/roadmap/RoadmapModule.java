package com.example.english_app.dto.response.roadmap;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /** Định danh ổn định trong một phiên bản roadmap. */
    private String moduleKey;

    /** Snapshot ID nội dung tại thời điểm roadmap được sinh. */
    private List<Long> contentItemIds;

    /** SHA-256 rút gọn của contentItemIds đã chuẩn hóa. */
    private String contentVersion;

    /**
     * Các trường dưới đây chỉ được enrich khi trả API, không lưu vào roadmap_json.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer doneCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double progressPercent;
}
