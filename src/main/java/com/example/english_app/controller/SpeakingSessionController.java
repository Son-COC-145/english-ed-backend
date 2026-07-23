package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.annotation.RateLimitedAi;
import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.service.speaking.SpeakingSessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/speaking-session")
@RequiredArgsConstructor
@Tag(name = "Speaking Sessions", description = "API cho tiến trình luyện tập nói")
public class SpeakingSessionController {
    private final SpeakingSessionService speakingSessionService;

    @Operation(summary = "Bắt đầu một phiên luyện nói mới")
    @PostMapping("/start")
    @RateLimitedAi
    public ResponseEntity<ApiResponse<SpeakingSessionResponse>> startSession(
            @Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(speakingSessionService.startSession(request)));
    }

    @Operation(summary = "Nhận file ghi âm của user, chuyển thành văn bản (STT) và lưu lịch sử")
    @PostMapping(value = "/{id}/audio-input", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AudioInputResponse>> uploadUserAudio(
            @PathVariable("id") Long sessionId,
            @RequestParam("file") MultipartFile audioFile) {

        AudioInputResponse response = speakingSessionService.processUserAudio(sessionId, audioFile);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
