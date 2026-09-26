package com.example.english_app.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentSubmissionDetailResponse {
    private AssignmentSubmissionResponse submission;
    /** Null when the referenced learning result no longer exists. */
    private SubmissionResultResponse result;
}
