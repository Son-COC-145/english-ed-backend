package com.example.english_app.dto.response.ipa;

import com.example.english_app.entity.enums.IpaMasteryStatus;
import com.example.english_app.entity.enums.PhonemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpaPhonemeMasteryDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Short id;
    private String symbol;
    private PhonemeType phonemeType;
    private String nameVi;
    private IpaMasteryStatus masteryStatus;
    private Short averageScore;
    private Short maxScore;
    private Long practiceCount;
    private Boolean isCommonVnError;
}
