package com.example.english_app.controller;

import com.example.english_app.dto.request.ipa.AdminExampleWordRequest;
import com.example.english_app.dto.request.ipa.AdminMinimalPairRequest;
import com.example.english_app.dto.request.ipa.AdminPhonemeUpdateRequest;
import com.example.english_app.dto.request.ipa.AdminPronunciationRuleRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.ipa.IpaExampleWordResponse;
import com.example.english_app.dto.response.ipa.IpaMinimalPairResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.PronunciationRuleDetailResponse;
import com.example.english_app.service.ipa.AdminIpaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ipa")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin IPA Management", description = "Quản trị viên thêm/sửa/xóa âm IPA, từ ví dụ, cặp âm đối lập và quy tắc ngữ âm")
public class AdminIpaController {

    private final AdminIpaService adminIpaService;

    // ─── 1. Quản lý Âm IPA ───────────────────────────────────────────────────

    @Operation(summary = "Cập nhật thông tin / cách đọc / mẹo phát âm của 1 âm IPA")
    @PutMapping("/phonemes/{id}")
    public ResponseEntity<ApiResponse<IpaPhonemeDetailResponse>> updatePhoneme(
            @PathVariable Short id,
            @Valid @RequestBody AdminPhonemeUpdateRequest request) {
        IpaPhonemeDetailResponse data = adminIpaService.updatePhoneme(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật âm IPA thành công"));
    }

    // ─── 2. Quản lý Từ ví dụ (Example Words) ─────────────────────────────────

    @Operation(summary = "Thêm từ vựng ví dụ mới cho 1 âm IPA")
    @PostMapping("/phonemes/{phonemeId}/words")
    public ResponseEntity<ApiResponse<IpaExampleWordResponse>> addExampleWord(
            @PathVariable Short phonemeId,
            @Valid @RequestBody AdminExampleWordRequest request) {
        IpaExampleWordResponse data = adminIpaService.addExampleWord(phonemeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Thêm từ ví dụ thành công"));
    }

    @Operation(summary = "Cập nhật từ vựng ví dụ (từ, phiên âm IPA, link audio)")
    @PutMapping("/words/{wordId}")
    public ResponseEntity<ApiResponse<IpaExampleWordResponse>> updateExampleWord(
            @PathVariable Long wordId,
            @Valid @RequestBody AdminExampleWordRequest request) {
        IpaExampleWordResponse data = adminIpaService.updateExampleWord(wordId, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật từ ví dụ thành công"));
    }

    @Operation(summary = "Xóa một từ vựng ví dụ của âm IPA")
    @DeleteMapping("/words/{wordId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteExampleWord(
            @PathVariable Long wordId) {
        adminIpaService.deleteExampleWord(wordId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Xóa từ ví dụ thành công")));
    }

    // ─── 3. Quản lý Cặp âm đối lập (Minimal Pairs) ───────────────────────────

    @Operation(summary = "Thêm mới một Cặp âm dễ gây nhầm lẫn (Minimal Pair)")
    @PostMapping("/minimal-pairs")
    public ResponseEntity<ApiResponse<IpaMinimalPairResponse>> createMinimalPair(
            @Valid @RequestBody AdminMinimalPairRequest request) {
        IpaMinimalPairResponse data = adminIpaService.createMinimalPair(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo cặp âm đối lập thành công"));
    }

    @Operation(summary = "Cập nhật Cặp âm dễ gây nhầm lẫn")
    @PutMapping("/minimal-pairs/{id}")
    public ResponseEntity<ApiResponse<IpaMinimalPairResponse>> updateMinimalPair(
            @PathVariable Long id,
            @Valid @RequestBody AdminMinimalPairRequest request) {
        IpaMinimalPairResponse data = adminIpaService.updateMinimalPair(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật cặp âm đối lập thành công"));
    }

    @Operation(summary = "Xóa một Cặp âm đối lập")
    @DeleteMapping("/minimal-pairs/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMinimalPair(
            @PathVariable Long id) {
        adminIpaService.deleteMinimalPair(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Xóa cặp âm đối lập thành công")));
    }

    // ─── 4. Quản lý Quy tắc Ngữ âm & Trọng âm (Rules) ────────────────────────

    @Operation(summary = "Thêm mới một Quy tắc Trọng âm / Nối âm / Ngữ điệu")
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<PronunciationRuleDetailResponse>> createPronunciationRule(
            @Valid @RequestBody AdminPronunciationRuleRequest request) {
        PronunciationRuleDetailResponse data = adminIpaService.createPronunciationRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo quy tắc phát âm thành công"));
    }

    @Operation(summary = "Cập nhật Quy tắc Trọng âm / Nối âm / Ngữ điệu")
    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<PronunciationRuleDetailResponse>> updatePronunciationRule(
            @PathVariable Long id,
            @Valid @RequestBody AdminPronunciationRuleRequest request) {
        PronunciationRuleDetailResponse data = adminIpaService.updatePronunciationRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật quy tắc phát âm thành công"));
    }

    @Operation(summary = "Xóa một Quy tắc phát âm")
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deletePronunciationRule(
            @PathVariable Long id) {
        adminIpaService.deletePronunciationRule(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Xóa quy tắc phát âm thành công")));
    }
}
