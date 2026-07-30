package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.dto.response.ipa.PhonemeScoreDto;
import com.example.english_app.dto.response.ipa.PronunciationResultResponse;
import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import com.example.english_app.event.PronunciationCompletedEvent;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.UserRepository;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.service.audio.AudioAssessmentPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Core service của Phase 2: nhận audio, gọi Azure AI, lưu log, publish event.
 *
 * <p><b>Transaction boundary:</b> Toàn bộ method bọc trong @Transactional.
 * Event được publish TRONG transaction — Spring sẽ thực sự gửi event NGAY SAU KHI
 * transaction commit (ApplicationEventPublisher mặc định là synchronous trong cùng TX,
 * nhưng Listener dùng @Async nên sẽ chạy trên thread khác với TX riêng của nó).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpaPronunciationServiceImpl implements IpaPronunciationService {

    // Ngưỡng XP thưởng
    private static final int XP_HIGH   = 20; // overallScore >= 80
    private static final int XP_MEDIUM = 15; // overallScore >= 60
    private static final int XP_LOW    = 10; // overallScore < 60 (vẫn thưởng vì đã cố gắng)

    private final AudioAssessmentPort          audioAssessmentPort;
    private final IpaExampleWordRepository     exampleWordRepository;
    private final PronunciationPracticeLogRepository practiceLogRepository;
    private final UserRepository               userRepository;
    private final ApplicationEventPublisher    eventPublisher;
    private final ObjectMapper                 objectMapper;

    @Override
    @Transactional
    public PronunciationResultResponse assess(Long studentId, Long exampleWordId, MultipartFile audio) {

        // 1. Lấy từ ví dụ (referenceText gửi Azure)
        IpaExampleWord exampleWord = exampleWordRepository.findById(exampleWordId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAMPLE_WORD_NOT_FOUND));

        // 2. Đọc bytes — magic byte validation nằm trong AudioAssessmentPort
        byte[] audioBytes = readBytes(audio);

        // 3. Gọi Azure AI
        PronunciationScoreResult azureResult =
                audioAssessmentPort.assess(audioBytes, exampleWord.getWord());

        // 4. Nếu Azure unavailable, trả về response nhưng không lưu log (không có điểm để lưu)
        if ("UNAVAILABLE".equals(azureResult.getStatus())) {
            throw new AppException(ErrorCode.PRONUNCIATION_UNAVAILABLE);
        }

        // 5. Tính stressCorrect (chỉ áp dụng cho từ > 1 âm tiết)
        // Azure không trả stress riêng ở mức word nếu chỉ dùng FullText granularity —
        // ta dùng PronScore >= 70 làm proxy heuristic cho single-word practice
        Boolean stressCorrect = calculateStressCorrect(exampleWord.getWord(), azureResult.getOverallScore());

        // 6. Build phoneme detail list — Azure ở granularity FullText không trả phoneme-level
        // Tạo single-entry list từ word-level score để giữ consistent response contract
        List<PhonemeScoreDto> phonemes = List.of(new PhonemeScoreDto(
                exampleWord.getIpaTranscription(),
                azureResult.getAccuracyScore(),
                azureResult.getScoreColor()
        ));

        // 7. Serialize phoneme detail thành JSON để lưu JSONB
        String phonemeJson = serializeToJson(phonemes);

        // 8. Lưu PronunciationPracticeLog
        PronunciationPracticeLog log = PronunciationPracticeLog.builder()
                .student(userRepository.getReferenceById(studentId))
                .practiceType(PracticeType.IPA_PHONEME)
                .refId(exampleWordId)
                .overallScore(azureResult.getOverallScore())
                .fluencyScore(azureResult.getFluencyScore())
                .completenessScore(azureResult.getCompletenessScore())
                .stressCorrect(stressCorrect)
                .phonemeDetailJson(phonemeJson)
                .build();

        PronunciationPracticeLog savedLog = practiceLogRepository.save(log);

        // 9. Publish gamification event (Async — chạy trên thread khác sau TX commit)
        int xp = calculateXp(azureResult.getOverallScore());
        eventPublisher.publishEvent(
                new PronunciationCompletedEvent(studentId, xp, savedLog.getId())
        );

        // 10. Build và trả về Response
        return new PronunciationResultResponse(
                azureResult.getOverallScore(),
                azureResult.getFluencyScore(),
                azureResult.getCompletenessScore(),
                stressCorrect,
                phonemes
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private byte[] readBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
    }

    /**
     * Heuristic: nếu từ có nhiều âm tiết (phát hiện qua đếm nguyên âm),
     * dùng overallScore >= 70 làm proxy cho "trọng âm đúng".
     * Nếu từ chỉ có 1 âm tiết → không có khái niệm trọng âm → null.
     */
    private Boolean calculateStressCorrect(String word, Short overallScore) {
        int syllableCount = countSyllables(word);
        if (syllableCount <= 1) return null;
        return overallScore != null && overallScore >= 70;
    }

    private int countSyllables(String word) {
        return (int) word.toLowerCase().chars()
                .filter(c -> "aeiou".indexOf(c) >= 0)
                .count();
    }

    private int calculateXp(Short overallScore) {
        if (overallScore == null) return XP_LOW;
        if (overallScore >= 80)   return XP_HIGH;
        if (overallScore >= 60)   return XP_MEDIUM;
        return XP_LOW;
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize phoneme detail to JSON", e);
            return "[]";
        }
    }
}
