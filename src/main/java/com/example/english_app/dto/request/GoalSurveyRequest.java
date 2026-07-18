package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalSurveyRequest {

    @NotBlank(message = "Mục đích học không được để trống")
    private String learningPurpose;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 kỹ năng muốn tập trung")
    @Size(min = 1, max = 5, message = "Chọn từ 1 đến 5 kỹ năng")
    private List<String> focusSkills;

    private Integer dailyStudyMinutes;

    @NotBlank(message = "Vui lòng chọn môi trường học")
    private String preferredEnvironment;

    @NotBlank(message = "Vui lòng chọn kinh nghiệm học trước đó")
    private String previousExperience;
}
