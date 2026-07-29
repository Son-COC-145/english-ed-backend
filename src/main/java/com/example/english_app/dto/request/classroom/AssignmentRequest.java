package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.ModuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Module type is required")
    private ModuleType moduleType;

    @NotNull(message = "Ref ID is required")
    private Long refId;

    private LocalDateTime deadlineAt;
}
