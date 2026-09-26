package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The current student's own submission shown next to an assignment; null when not submitted yet. */
@Data
@Builder
public class MySubmissionResponse {
    private Long id;
    private AssignmentSubmissionStatus status;
    /** Only set once the submission is GRADED. */
    private BigDecimal score;
    private LocalDateTime submittedAt;
    private LocalDateTime commentedAt;
    private boolean hasTeacherComment;
}
