package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentSubmissionResponse {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private Long resultRefId;
    private BigDecimal score;
    private AssignmentSubmissionStatus status;
    private LocalDateTime submittedAt;
    private String teacherCommentText;
    private String teacherAudioCommentUrl;
    private LocalDateTime commentedAt;
    private LocalDateTime updatedAt;
}
