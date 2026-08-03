package com.example.english_app.controller;

import com.example.english_app.dto.request.VocabularyRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.service.vocabulary.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/vocabulary")
@RequiredArgsConstructor
@Tag(name = "Vocabulary", description = "Quản lý từ vựng")
public class VocabularyController {
    private final VocabularyService vocabularyService;

    @Operation(summary = "Lấy chi tiết từ vựng theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> getById(@PathVariable("id") Long id) {
        VocabularyResponse response = vocabularyService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách từ vựng (Có phân trang và lọc)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VocabularyResponse>>> getAll(
            @RequestParam(required = false) Short topicId,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) VocabularyStatus status,
            @RequestParam(required = false) CefrLevel cefrLevel,
            @RequestParam(required = false) String wordSearch,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        PageResponse<VocabularyResponse> response = vocabularyService.filterVocabularies(
                topicId, createdById, status, cefrLevel, wordSearch, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Tạo từ vựng mới")
    @PostMapping
    public ResponseEntity<ApiResponse<VocabularyResponse>> create(@Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Cập nhật từ vựng")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Xóa từ vựng")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable("id") Long id) {
        vocabularyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa từ vựng thành công"));
    }
}
