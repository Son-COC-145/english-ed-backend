package com.example.english_app.dto.request.ipa;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExampleWordRequest {

    @NotBlank(message = "Từ vựng không được để trống")
    private String word;

    @NotBlank(message = "Phiên âm IPA không được để trống")
    private String ipaTranscription;

    /** URL audio pre-generated. Có thể null — khi null FE sẽ dùng TTS fallback. */
    private String audioUrl;

    /** Nghĩa tiếng Việt của từ. */
    private String meaningVi;

    /** URL ảnh minh họa nghĩa của từ. */
    private String imageUrl;
}
