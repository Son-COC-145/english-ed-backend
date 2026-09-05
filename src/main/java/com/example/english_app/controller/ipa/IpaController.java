package com.example.english_app.controller.ipa;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.ipa.*;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.enums.PronunciationRuleCategory;
import com.example.english_app.service.ipa.IpaPronunciationService;
import com.example.english_app.service.ipa.IpaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ipa/phonemes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // BUG-03: Bảo vệ toàn bộ controller
@Tag(name = "IPA Phonemes", description = "Module 1: Học phát âm IPA & Quy tắc ngữ âm")
public class IpaController {

    private final IpaService              ipaService;
    private final IpaPronunciationService ipaPronunciationService;
    private final com.example.english_app.service.audio.AzureTtsService azureTtsService;

    @Operation(summary = "Phát âm thanh động qua Azure TTS (Fallback tức thì nếu từ thiếu audio)")
    @GetMapping(value = "/tts/stream", produces = "audio/mpeg")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> streamTts(
            @RequestParam String text,
            @RequestParam(defaultValue = "WORD") String type,
            @RequestParam(defaultValue = "MALE") String voice) {

        if (text == null || text.trim().isEmpty()) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.INVALID_REQUEST, "text không được để trống");
        }
        if (text.length() > 150) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.INVALID_REQUEST, "text không được vượt quá 150 ký tự");
        }

        if (!"WORD".equalsIgnoreCase(type) && !"PHONEME".equalsIgnoreCase(type)) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.INVALID_REQUEST, "type phải là 'WORD' hoặc 'PHONEME'");
        }

        if (!"MALE".equalsIgnoreCase(voice) && !"FEMALE".equalsIgnoreCase(voice)) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.INVALID_REQUEST, "voice phải là 'MALE' hoặc 'FEMALE'");
        }

        String voiceName = "FEMALE".equalsIgnoreCase(voice) 
                ? com.example.english_app.service.audio.AzureTtsService.VOICE_FEMALE_US 
                : com.example.english_app.service.audio.AzureTtsService.VOICE_MALE_US;
        
        byte[] audioBytes = "PHONEME".equalsIgnoreCase(type)
                ? azureTtsService.synthesizePhoneme(text.trim(), voiceName)
                : azureTtsService.synthesizeWord(text.trim(), voiceName);
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(audioBytes);
    }

    // ─── Phase 1: IPA Sound Library & Mastery ────────────────────────────────

    @Operation(summary = "Lấy danh sách tất cả các âm IPA")
    @GetMapping
    public ResponseEntity<ApiResponse<List<IpaPhonemeResponse>>> getAllPhonemes(
            @RequestParam(required = false) PhonemeType type,
            @RequestParam(required = false) Boolean isCommonError) {
        List<IpaPhonemeResponse> data = ipaService.getAllPhonemes(type, isCommonError);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Lấy bảng nhiệt độ thành thạo 44 âm IPA của người dùng (Mastery Heatmap)")
    @GetMapping("/mastery")
    public ResponseEntity<ApiResponse<IpaMasterySummaryResponse>> getPhonemeMastery(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(ipaService.getPhonemeMastery(userId)));
    }

    @Operation(summary = "Lấy danh sách các cặp âm dễ gây nhầm lẫn (Minimal Pairs)")
    @GetMapping("/minimal-pairs")
    public ResponseEntity<ApiResponse<List<IpaMinimalPairResponse>>> getMinimalPairs() {
        return ResponseEntity.ok(ApiResponse.success(ipaService.getMinimalPairs()));
    }

    @Operation(summary = "Lấy danh sách các Quy tắc Trọng âm & Ghép âm / Nối âm (Pronunciation Rules)")
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<PronunciationRuleResponse>>> getPronunciationRules(
            @RequestParam(required = false) PronunciationRuleCategory category) {
        return ResponseEntity.ok(ApiResponse.success(ipaService.getPronunciationRules(category)));
    }

    @Operation(summary = "Lấy chi tiết một Quy tắc Ngữ âm (kèm ví dụ, audio mẫu & giải thích)")
    @GetMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<PronunciationRuleDetailResponse>> getPronunciationRuleDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ipaService.getPronunciationRuleDetail(id)));
    }

    @Operation(summary = "Lấy chi tiết 1 âm IPA (kèm video khẩu hình, gợi ý sửa lỗi, từ ví dụ & trạng thái bookmark)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IpaPhonemeDetailResponse>> getPhonemeDetail(
            @PathVariable Short id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        IpaPhonemeDetailResponse data = ipaService.getPhonemeDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Lấy lịch sử luyện tập phát âm gần nhất của 1 âm IPA")
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<IpaPracticeHistoryResponse>>> getPracticeHistory(
            @PathVariable Short id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(ipaService.getPracticeHistory(userId, id)));
    }

    @Operation(summary = "Lưu / Bỏ lưu âm IPA (Bookmark toggle) — trả về typed response")
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<ApiResponse<com.example.english_app.dto.response.ipa.IpaBookmarkResponse>> toggleBookmark(
            @PathVariable Short id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        com.example.english_app.dto.response.ipa.IpaBookmarkResponse data = ipaService.toggleBookmark(userId, id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Lấy danh sách âm IPA đã lưu (Bookmark list) – sắp xếp mới nhất trước")
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<com.example.english_app.dto.response.ipa.IpaPhonemeBookmarkResponse>>> getBookmarkedPhonemes(
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(ipaService.getBookmarkedPhonemes(userId)));
    }

    // ─── Phase 2: AI Assessment Engine ───────────────────────────────────────

    /**
     * POST /api/v1/ipa/phonemes/practice
     * Nhận audio + exampleWordId, trả về kết quả chấm điểm phát âm chi tiết.
     *
     * <p>File size đã được giới hạn ở tầng Spring filter (max-file-size=5MB).
     * Magic byte validation nằm trong AudioAssessmentPort.
     */
    @Operation(summary = "Luyện tập phát âm từ ví dụ (Gửi file audio chấm điểm AI)")
    @PostMapping(value = "/practice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PronunciationResultResponse>> practice(
            @RequestParam Long exampleWordId,
            @RequestParam MultipartFile audio,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        PronunciationResultResponse data = ipaPronunciationService.assess(userId, exampleWordId, audio);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * SECURITY-01: Guard null claim — throw 401 thay vì NullPointerException.
     * JWT claim "userId" có thể không tồn tại nếu token bị forge hoặc issue sai.
     */
    private Long extractUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number userId = jwt.getClaim("userId");
        if (userId == null) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.UNAUTHORIZED);
        }
        return userId.longValue();
    }
}
