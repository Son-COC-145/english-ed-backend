package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.english_app.dto.request.MinigameSubmitRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.MinigameSubmitResponse;
import com.example.english_app.service.GameficationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/vocabularies/minigames")
@RequiredArgsConstructor
@Tag(name = "Mini Game", description = "API endpoints for mini games.")
public class MiniGameController {

    private final GameficationService gameficationService;

    @PostMapping("/submit")
    @Operation(summary = "Submit minigame result", description = "Calculates XP, updates streaks and word progress")
    public ResponseEntity<ApiResponse<MinigameSubmitResponse>> submitResult(
            @Valid @RequestBody MinigameSubmitRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.procesGameSubmit(request)));
    }
}
