package com.example.english_app.dto.request.ipa;

import com.example.english_app.entity.enums.PronunciationRuleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPronunciationRuleRequest {

    @NotNull(message = "Category không được để trống")
    private PronunciationRuleCategory category;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String titleVi;

    @NotBlank(message = "Tóm tắt không được để trống")
    private String summaryVi;

    @NotBlank(message = "Nội dung markdown không được để trống")
    private String contentMarkdown;

    private String examplesJson;
    private Integer orderIndex;
    private Boolean isActive;
}
