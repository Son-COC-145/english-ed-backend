package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradeRequest {
    private BigDecimal score;

    @NotNull(message = "Status is required")
    private AssignmentSubmissionStatus status;

    private String teacherCommentText;

    private String teacherAudioCommentUrl;
}
