package com.example.english_app.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeachingMaterialRequest {
    @NotBlank(message = "Title is required")
    private String title;
}
