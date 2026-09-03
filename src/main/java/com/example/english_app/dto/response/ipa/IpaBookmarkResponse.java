package com.example.english_app.dto.response.ipa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response cho thao tac bookmark am IPA.
 * Thay the Map<String, Boolean> de Frontend co typed schema ro rang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpaBookmarkResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Short phonemeId;
    private boolean isBookmarked;
}
