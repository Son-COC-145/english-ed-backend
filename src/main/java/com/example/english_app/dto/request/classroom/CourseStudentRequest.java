package com.example.english_app.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseStudentRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;
}
