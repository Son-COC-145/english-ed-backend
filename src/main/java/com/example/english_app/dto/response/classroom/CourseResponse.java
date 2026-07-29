package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.CefrLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String name;
    private Long teacherId;
    private String description;
    private CefrLevel cefrTarget;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
