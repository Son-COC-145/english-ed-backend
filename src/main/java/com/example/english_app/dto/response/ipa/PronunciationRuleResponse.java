package com.example.english_app.dto.response.ipa;

import com.example.english_app.entity.enums.PronunciationRuleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationRuleResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private PronunciationRuleCategory category;
    private String titleVi;
    private String summaryVi;
    private Integer orderIndex;
}
