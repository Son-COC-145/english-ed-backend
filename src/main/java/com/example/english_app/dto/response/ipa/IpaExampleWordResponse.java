package com.example.english_app.dto.response.ipa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response cho từ ví dụ của một âm IPA.
 *
 * <p><b>Hướng dẫn sử dụng audio cho Frontend:</b>
 * <ol>
 *   <li>Ưu tiên dùng {@code audioUrl} nếu không null (file pre-generated trên Azure Blob, chất lượng cao).
 *   <li>Nếu {@code audioUrl} null, dùng {@code ttsUrl} — luôn luôn có giá trị,
 *       gọi Azure TTS stream tức thì để phát âm từ đó (độ trễ ~200ms).
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class IpaExampleWordResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String word;
    private String ipaTranscription;

    /** Pre-generated audio URL (Azure Blob). Có thể null nếu chưa sinh audio. */
    private String audioUrl;

    /**
     * TTS fallback URL – luôn được tính sẵn, không bao giờ null.
     * Frontend chỉ cần: {@code final url = audioUrl ?? ttsUrl}
     */
    private String ttsUrl;

    /** Static factory để giữ backward-compatible khi gọi new IpaExampleWordResponse(id, word, ipa, audioUrl). */
    public static IpaExampleWordResponse of(Long id, String word, String ipaTranscription, String audioUrl) {
        String tts = buildTtsUrl(word, "MALE");
        return IpaExampleWordResponse.builder()
                .id(id)
                .word(word)
                .ipaTranscription(ipaTranscription)
                .audioUrl(audioUrl)
                .ttsUrl(tts)
                .build();
    }

    /** Sinh TTS URL dạng relative path. Frontend ghép với base URL của server. */
    public static String buildTtsUrl(String text, String voice) {
        return "/api/v1/ipa/phonemes/tts/stream?text=" +
               java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8) +
               "&type=WORD&voice=" + voice;
    }
}
