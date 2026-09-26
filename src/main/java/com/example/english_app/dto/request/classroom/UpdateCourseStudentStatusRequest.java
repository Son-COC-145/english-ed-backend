package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.ClassStudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseStudentStatusRequest {
    @NotNull(message = "Status is required")
    private ClassStudentStatus status;
}
