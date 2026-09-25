package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapGenerationStatusResponse {
    private String status;
    private boolean ready;
    private Integer attempts;
    private Long retryAfterMs;
}
