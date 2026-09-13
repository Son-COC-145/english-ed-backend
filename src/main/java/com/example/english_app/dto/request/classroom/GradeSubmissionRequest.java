package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSubmissionRequest {
    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "100.0", message = "Score must not exceed 100")
    private BigDecimal score;

    @NotNull(message = "Status is required")
    private AssignmentSubmissionStatus status;

    private String teacherCommentText;

    private String teacherAudioCommentUrl;
}
