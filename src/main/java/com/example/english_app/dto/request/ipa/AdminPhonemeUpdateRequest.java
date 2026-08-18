package com.example.english_app.dto.request.ipa;

import com.example.english_app.entity.enums.CefrLevel;
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
public class AdminPhonemeUpdateRequest {

    @NotBlank(message = "Tên tiếng Việt không được để trống")
    private String nameVi;

    private String audioMaleUrl;
    private String audioFemaleUrl;
    private String videoMouthUrl;

    @NotNull(message = "Trình độ CEFR không được để trống")
    private CefrLevel cefrIntroLevel;

    private Boolean isCommonVnError;
    private String pronunciationTipVi;
}
