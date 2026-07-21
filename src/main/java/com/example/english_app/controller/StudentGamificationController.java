package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.MinigameResultDetailResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.StudentVocabularyProgressResponse;
import com.example.english_app.service.GameficationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Gamification Progress", description = "Lấy tiến trình học tập, trạng thái từ vựng và lịch sử trò chơi của học viên")
public class StudentGamificationController {

    private final GameficationService gameficationService;

    @Operation(summary = "Lấy thông số học tập (StudentStat) của người dùng hiện tại")
    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<StudentStatResponse>> getStudentStat() {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getStudentStat()));
    }

    @Operation(summary = "Lấy danh sách tiến trình học từ vựng (phân trang)")
    @GetMapping("/vocabulary-progress")
    public ResponseEntity<ApiResponse<PageResponse<StudentVocabularyProgressResponse>>> getVocabularyProgresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastPracticedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getVocabularyProgresses(pageable)));
    }

    @Operation(summary = "Xem chi tiết một tiến trình học từ vựng")
    @GetMapping("/vocabulary-progress/{id}")
    public ResponseEntity<ApiResponse<StudentVocabularyProgressResponse>> getVocabularyProgressDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getVocabularyProgress(id)));
    }

    @Operation(summary = "Lấy danh sách lịch sử chơi minigame (phân trang)")
    @GetMapping("/minigame-results")
    public ResponseEntity<ApiResponse<PageResponse<MinigameResultDetailResponse>>> getMinigameResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "playedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getMinigameResults(pageable)));
    }

    @Operation(summary = "Xem chi tiết một kết quả chơi minigame")
    @GetMapping("/minigame-results/{id}")
    public ResponseEntity<ApiResponse<MinigameResultDetailResponse>> getMinigameResultDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getMinigameResult(id)));
    }
}
