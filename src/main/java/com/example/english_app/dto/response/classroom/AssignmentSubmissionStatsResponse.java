package com.example.english_app.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

/** Submission counters of one assignment, counted only for students currently ACTIVE in the course. */
@Data
@Builder
public class AssignmentSubmissionStatsResponse {
    private Long activeStudents;
    /** All submissions (SUBMITTED + LATE + GRADED). */
    private Long submittedCount;
    /** Late submissions not graded yet; grading replaces LATE with GRADED. */
    private Long lateCount;
    private Long gradedCount;
    private Long notSubmittedCount;
}
