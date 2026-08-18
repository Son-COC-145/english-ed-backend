package com.example.english_app.dto.request.ipa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMinimalPairRequest {

    @NotNull(message = "Phoneme 1 ID không được để trống")
    private Short phoneme1Id;

    @NotNull(message = "Phoneme 2 ID không được để trống")
    private Short phoneme2Id;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Từ 1 không được để trống")
    private String word1;

    @NotBlank(message = "IPA 1 không được để trống")
    private String ipa1;

    private String audio1Url;

    @NotBlank(message = "Từ 2 không được để trống")
    private String word2;

    @NotBlank(message = "IPA 2 không được để trống")
    private String ipa2;

    private String audio2Url;
}
