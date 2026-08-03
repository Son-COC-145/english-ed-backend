package com.example.english_app.dto.response.classroom;

import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.enums.ClassStudentStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CourseStudentDetailResponse {
    private Long id;
    private Long courseId;
    private ClassStudentStatus status;
    private LocalDateTime joinedAt;

    // Thông tin chi tiết kết hợp
    private UserResponse studentInfo;
    private StudentStatResponse studentProgress;
}
