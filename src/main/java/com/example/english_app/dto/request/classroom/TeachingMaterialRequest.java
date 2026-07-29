package com.example.english_app.dto.request.classroom;

import com.example.english_app.entity.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingMaterialRequest {
    private Long courseId; // null if personal material

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "File type is required")
    private FileType fileType;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private Integer fileSizeKb;
    
    @Builder.Default
    private Boolean isLivePresenting = false;
}
