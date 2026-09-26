package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Digits(integer = 3, fraction = 2, message = "Score must have at most 2 decimal places")
    private BigDecimal score;

    @NotNull(message = "Status is required")
    private AssignmentSubmissionStatus status;

    @Size(max = 5000, message = "Comment must not exceed 5000 characters")
    private String teacherCommentText;

    @Size(max = 500, message = "Audio comment URL must not exceed 500 characters")
    @Pattern(regexp = "^https://\\S+$", message = "Audio comment URL must be an HTTPS URL")
    private String teacherAudioCommentUrl;
}
