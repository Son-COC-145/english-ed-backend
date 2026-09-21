package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.ReviewRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
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
    @Min(value = 0, message = "Duration Seconds cannot be negative")
    private Integer durationSeconds;
    @NotBlank(message = "attemptId is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$", message = "attemptId must be a valid UUID")
    private String attemptId;
}
