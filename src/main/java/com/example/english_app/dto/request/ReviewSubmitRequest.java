package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.ReviewRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitRequest {

    @NotNull(message = "Vocabulary ID is required")
    private Long vocabularyId;

    @NotNull(message = "Rating is required")
    private ReviewRating rating;

    private Short durationSeconds;

    @Size(min = 36, max = 36, message = "attemptId phải là UUID 36 ký tự")
    private String attemptId;
}
