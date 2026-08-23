package com.example.english_app.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.example.english_app.dto.response.TopicResponse;

@Data
@Builder
public class SyllabusItemResponse {
    private Long id;
    private Long courseId;
    private Short weekNumber;
    private String title;
    private String description;
    private LocalDate scheduledDate;
    private Long materialId;
    private Short sortOrder;
    private List<TopicResponse> topics;
    private LocalDateTime updatedAt;
}
