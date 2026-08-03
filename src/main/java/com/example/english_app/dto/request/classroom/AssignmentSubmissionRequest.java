package com.example.english_app.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionRequest {
    @NotNull(message = "Result Ref ID is required")
    private Long resultRefId;
}
