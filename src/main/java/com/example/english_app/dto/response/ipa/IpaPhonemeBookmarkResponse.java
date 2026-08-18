package com.example.english_app.dto.response.ipa;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trả về khi học viên xem danh sách âm đã bookmark.
 * Gộp thông tin phoneme + thời điểm bookmark để Frontend sắp xếp / hiển thị.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpaPhonemeBookmarkResponse {

    private Short phonemeId;
    private String symbol;
    private PhonemeType phonemeType;
    private String nameVi;
    private String audioMaleUrl;
    private String audioFemaleUrl;
    private Boolean isCommonVnError;
    private CefrLevel cefrIntroLevel;
    private String pronunciationTipVi;

    /** Thời điểm học viên đánh dấu âm này (sắp xếp mới nhất trước). */
    private LocalDateTime bookmarkedAt;
}
