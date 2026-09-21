package com.example.english_app.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseTeacherResponse {
    private Long id;
    private String fullName;
    private String email;
    private String avatarUrl;
}
