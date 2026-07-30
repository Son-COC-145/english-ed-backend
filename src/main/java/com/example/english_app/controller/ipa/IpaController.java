package com.example.english_app.controller.ipa;

import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeResponse;
import com.example.english_app.dto.response.ipa.PronunciationResultResponse;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.service.ipa.IpaPronunciationService;
import com.example.english_app.service.ipa.IpaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ipa/phonemes")
@RequiredArgsConstructor
public class IpaController {

    private final IpaService              ipaService;
    private final IpaPronunciationService ipaPronunciationService;

    // ─── Phase 1: IPA Sound Library ──────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPhonemes(
            @RequestParam(required = false) PhonemeType type,
            @RequestParam(required = false) Boolean isCommonError) {
        List<IpaPhonemeResponse> data = ipaService.getAllPhonemes(type, isCommonError);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPhonemeDetail(@PathVariable Short id) {
        IpaPhonemeDetailResponse data = ipaService.getPhonemeDetail(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Short id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        Map<String, Boolean> data = ipaService.toggleBookmark(userId, id);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    // ─── Phase 2: AI Assessment Engine ───────────────────────────────────────

    /**
     * POST /api/v1/ipa/phonemes/practice
     * Nhận audio + exampleWordId, trả về kết quả chấm điểm phát âm chi tiết.
     *
     * <p>File size đã được giới hạn ở tầng Spring filter (max-file-size=5MB).
     * Magic byte validation nằm trong AudioAssessmentPort.
     */
    @PostMapping(value = "/practice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> practice(
            @RequestParam Long exampleWordId,
            @RequestParam MultipartFile audio,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        PronunciationResultResponse data = ipaPronunciationService.assess(userId, exampleWordId, audio);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Long extractUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
