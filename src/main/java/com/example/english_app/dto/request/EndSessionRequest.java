package com.example.english_app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndSessionRequest {
    private Integer totalSpeakingTimeSeconds;
}
