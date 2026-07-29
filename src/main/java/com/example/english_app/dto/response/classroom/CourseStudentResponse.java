package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.ClassStudentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseStudentResponse {
    private Long id;
    private Long courseId;
    private Long studentId;
    private ClassStudentStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}
