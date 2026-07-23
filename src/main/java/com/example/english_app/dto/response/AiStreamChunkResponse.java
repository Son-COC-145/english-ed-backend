package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunkResponse {
    private String textChunk;
    private String audioBase64;
    private boolean isFinal;
}
