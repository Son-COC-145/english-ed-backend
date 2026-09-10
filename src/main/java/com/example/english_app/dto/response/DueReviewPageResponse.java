package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Paginated response cho SRS due-review queue */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueReviewPageResponse {
    private long dueCount;
    private List<VocabularyDueReviewResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
