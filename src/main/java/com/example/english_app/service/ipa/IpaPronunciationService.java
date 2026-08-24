package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.PronunciationResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IpaPronunciationService {

    /**
     * Đánh giá phát âm của học viên cho một từ ví dụ IPA.
     *
     * @param studentId     ID học viên (từ JWT)
     * @param exampleWordId ID của IpaExampleWord cần luyện
     * @param audio         File ghi âm (wav/webm/ogg, max 5MB — đã được filter ở Controller)
     * @return Kết quả chi tiết với color-code từng âm vị
     */
    PronunciationResultResponse assess(Long studentId, Long exampleWordId, MultipartFile audio);
}
