package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalSurveyRequest {

    @NotNull(message = "Mục tiêu học không được để trống")
    @Schema(description = "Mục tiêu học chính", example = "COMMUNICATION")
    private LearningGoal learningGoal;

    @Size(max = 200, message = "Mô tả mục tiêu khác không được vượt quá 200 ký tự")
    @Schema(description = "Mô tả bổ sung, không dùng để tính roadmap", maxLength = 200)
    private String otherGoalText;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 kỹ năng muốn tập trung")
    @Size(min = 1, max = 3, message = "Chọn từ 1 đến 3 kỹ năng")
    @Valid
    @Schema(description = "Các kỹ năng ưu tiên", example = "[\"SPEAKING\", \"PRONUNCIATION\"]")
    private List<@NotNull(message = "Mã kỹ năng không được để trống") LearnerSkill> focusSkills;

    @Min(value = 5, message = "Thời gian học mỗi ngày tối thiểu là 5 phút")
    @Max(value = 120, message = "Thời gian học mỗi ngày tối đa là 120 phút")
    private Integer dailyStudyMinutes;

    @NotBlank(message = "Vui lòng chọn môi trường học")
    private String preferredEnvironment;

    @NotBlank(message = "Vui lòng chọn kinh nghiệm học trước đó")
    private String previousExperience;
}
