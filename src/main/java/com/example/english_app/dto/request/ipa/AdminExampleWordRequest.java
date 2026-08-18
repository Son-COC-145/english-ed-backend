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

    @NotBlank(message = "Đường dẫn audio không được để trống")
    private String audioUrl;
}
