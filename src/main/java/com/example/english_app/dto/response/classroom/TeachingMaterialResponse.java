package com.example.english_app.dto.response.classroom;

import com.example.english_app.entity.enums.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeachingMaterialResponse {
    private Long id;
    private Long teacherId;
    private Long courseId;
    private String title;
    private FileType fileType;
    private String fileUrl;
    private Integer fileSizeKb;
    private Boolean isLivePresenting;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
