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
public class IpaMinimalPairResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;

    private Short phoneme1Id;
    private String phoneme1Symbol;
    private String word1;
    private String ipa1;
    private String audio1Url;

    private Short phoneme2Id;
    private String phoneme2Symbol;
    private String word2;
    private String ipa2;
    private String audio2Url;
}
