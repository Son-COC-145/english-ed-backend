package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.english_app.annotation.RateLimitedAi;
import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.service.speaking.SpeakingSessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
