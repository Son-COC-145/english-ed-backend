package com.example.english_app.dto.response.ipa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationRuleExampleDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String word;
    private String ipa;
    private String meaning;
    private String audioUrl;
}
