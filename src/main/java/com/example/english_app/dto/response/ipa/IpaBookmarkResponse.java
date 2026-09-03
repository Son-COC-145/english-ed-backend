package com.example.english_app.dto.response.ipa;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /**
     * @JsonProperty bắt buộc: Lombok @Data sinh getter isBookmarked() → Jackson
     * bỏ prefix "is" → serialize thành "bookmarked". Annotation này giữ đúng key
     * "isBookmarked" trong JSON để FE khớp với contract đã thoả thuận.
     */
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
}
