package com.example.english_app.controller;

import com.example.english_app.annotation.RateLimitedAi;
import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.service.speaking.AiStreamingService;
import com.example.english_app.service.speaking.SpeakingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/speaking-session")
@RequiredArgsConstructor
@Tag(name = "Speaking Sessions", description = "API cho tiến trình luyện tập nói")
public class SpeakingSessionController {

    private final SpeakingSessionService sessions;
    private final AiStreamingService streaming;

    public record TextInput(
            @NotBlank
            @Size(max = 4000)
            String transcript
    ) {}

    @Operation(summary = "Bắt đầu một phiên luyện nói mới")
    @PostMapping("/start")
    @RateLimitedAi
    public ResponseEntity<ApiResponse<SpeakingSessionResponse>> start(
            @Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(sessions.startSession(request)));
    }

    @Operation(summary = "Gửi file ghi âm của người dùng, thực hiện STT và lên lịch xử lý lượt nói")
    @PostMapping(value = "/{id}/audio-input", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimitedAi
    public ResponseEntity<ApiResponse<AudioInputResponse>> audio(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(sessions.processUserAudio(id, key, file)));
    }

    @Operation(summary = "Gửi tin nhắn dạng text thay cho audio")
    @PostMapping("/{id}/text-input")
    @RateLimitedAi
    public ResponseEntity<ApiResponse<AudioInputResponse>> text(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TextInput body) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(sessions.processText(id, key, body.transcript())));
    }

    @Operation(summary = "Đăng ký nhận phản hồi AI dạng stream (SSE)")
    @GetMapping(value = "/{id}/stream-response", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        return streaming.streamResponse(id);
    }

    @Operation(summary = "Kết thúc phiên luyện nói và yêu cầu đánh giá toàn bộ phiên")
    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<SessionEvaluationResponse>> end(@PathVariable Long id) {
        var report = sessions.endSession(id);
        int statusCode = "COMPLETED".equals(report.getStatus()) ? 200 : 202;
        return ResponseEntity.status(statusCode).body(ApiResponse.success(report));
    }

    @Operation(summary = "Xem báo cáo và kết quả đánh giá phiên luyện nói")
    @GetMapping("/{id}/report")
    public ResponseEntity<ApiResponse<SessionEvaluationResponse>> report(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sessions.report(id)));
    }

    @Operation(summary = "Lấy gợi ý câu thoại cho phiên nói")
    @PostMapping("/{id}/hints")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hint(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("phrases", sessions.hint(id, key))));
    }

    @Operation(summary = "Thử lại xử lý phiên nói khi gặp lỗi")
    @PostMapping("/{id}/retry")
    @RateLimitedAi
    public ResponseEntity<ApiResponse<String>> retry(@PathVariable Long id) {
        sessions.retry(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Đã lên lịch xử lý lại"));
    }

    @Operation(summary = "Lấy dữ liệu audio của một lượt nói")
    @GetMapping("/{id}/turns/{turnId}/audio")
    public ResponseEntity<byte[]> audioData(
            @PathVariable Long id,
            @PathVariable Long turnId) {
        var turn = sessions.audio(id, turnId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(turn.getAudioContentType()))
                .body(turn.getAudioData());
    }

    @PostMapping("/{id}/turns/{turnId}/retry")
    @RateLimitedAi
    public ResponseEntity<ApiResponse<String>> retryTurn(@PathVariable Long id, @PathVariable Long turnId) {
        sessions.retryTurn(id, turnId);
        return ResponseEntity.accepted().body(ApiResponse.success("Retry scheduled"));
    }
}
