package com.example.english_app.controller;

import com.example.english_app.dto.request.VocabularyRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.service.vocabulary.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Vocabulary", description = "Quản lý từ vựng và tra cứu")
public class VocabularyController {
    private final VocabularyService vocabularyService;

    @Operation(
        summary = "Lấy chi tiết từ vựng theo ID",
        description = "Trả về thông tin chi tiết từ vựng bao gồm câu ví dụ, collocations, ngữ cảnh hội thoại đã được phân tích dạng mảng đối tượng (typed). Nếu người dùng là học viên, trả kèm tiến trình học (userProgress)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> getById(
            @Parameter(description = "ID từ vựng", example = "101")
            @PathVariable("id") Long id) {
        VocabularyResponse response = vocabularyService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "Lấy danh sách từ vựng (Có phân trang và lọc)",
        description = "Hỗ trợ lọc theo topicId, createdById, status, cefrLevel và tìm kiếm từ. " +
                      "Lưu ý bảo mật: Với học viên (ROLE_STUDENT), hệ thống chỉ trả vocabulary PUBLISHED thuộc topic active, bất kể tham số truyền vào."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VocabularyResponse>>> getAll(
            @Parameter(description = "Lọc theo ID chủ đề")
            @RequestParam(required = false) Short topicId,
            @Parameter(description = "Lọc theo ID người tạo")
            @RequestParam(required = false) Long createdById,
            @Parameter(description = "Lọc theo trạng thái (chỉ có tác dụng với ADMIN/TEACHER)")
            @RequestParam(required = false) VocabularyStatus status,
            @Parameter(description = "Lọc theo khung CEFR (A1, A2, B1, B2, C1, C2)")
            @RequestParam(required = false) CefrLevel cefrLevel,
            @Parameter(description = "Từ khóa tìm kiếm theo từ tiếng Anh")
            @RequestParam(required = false) String wordSearch,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        PageResponse<VocabularyResponse> response = vocabularyService.filterVocabularies(
                topicId, createdById, status, cefrLevel, wordSearch, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Tạo từ vựng mới (Dành cho ADMIN hoặc TEACHER)")
    @PostMapping
    public ResponseEntity<ApiResponse<VocabularyResponse>> create(@Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Cập nhật từ vựng (Dành cho ADMIN hoặc TEACHER)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> update(
            @Parameter(description = "ID từ vựng cần cập nhật", example = "101")
            @PathVariable("id") Long id,
            @Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Xóa từ vựng (Dành cho ADMIN hoặc TEACHER)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @Parameter(description = "ID từ vựng cần xóa", example = "101")
            @PathVariable("id") Long id) {
        vocabularyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa từ vựng thành công"));
    }
}
