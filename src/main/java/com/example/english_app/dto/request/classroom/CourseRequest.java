package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {
    @NotBlank(message = "Course name is required")
    private String name;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private String description;
    
    private CefrLevel cefrTarget;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @Builder.Default
    private Boolean isActive = true;
}
