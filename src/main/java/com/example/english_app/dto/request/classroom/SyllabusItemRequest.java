package com.example.english_app.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusItemRequest {


    @NotNull(message = "Week number is required")
    private Short weekNumber;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDate scheduledDate;

    private Long materialId;

    @Builder.Default
    private Short sortOrder = 0;

    private List<Short> topicIds;
}
