package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.ModuleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentResponse {
    private Long id;
    private Long courseId;
    private Long teacherId;
    private String title;
    private String description;
    private ModuleType moduleType;
    private Long refId;
    private LocalDateTime deadlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Only filled for teacher views; null for students. */
    private AssignmentSubmissionStatsResponse submissionStats;
    /** Filled in the student cross-course list (GET /student/assignments). */
    private String courseName;
    /** Only filled for student views: the student's own submission, null when not submitted yet. */
    private MySubmissionResponse mySubmission;
}
