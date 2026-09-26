package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.english_app.dto.request.MinigameRoundStartRequest;
import com.example.english_app.dto.request.MinigameSubmitRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.MinigameRoundResponse;
import com.example.english_app.dto.response.MinigameSubmitResponse;
import com.example.english_app.service.gamification.GameficationService;
import com.example.english_app.service.gamification.MinigameRoundService;

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
    private final MinigameRoundService minigameRoundService;

    @PostMapping("/submit")
    @Operation(summary = "Submit minigame result", description = "Calculates XP, updates streaks and word progress")
    public ResponseEntity<ApiResponse<MinigameSubmitResponse>> submitResult(
            @Valid @RequestBody MinigameSubmitRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.procesGameSubmit(request)));
    }

    @PostMapping("/rounds")
    @Operation(summary = "Start a mini-game round", description = "Creates an IN_PROGRESS round for a topic; send its id as roundId with each answer")
    public ResponseEntity<ApiResponse<MinigameRoundResponse>> startRound(
            @Valid @RequestBody MinigameRoundStartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(minigameRoundService.startRound(request)));
    }

    @GetMapping("/rounds/{roundId}")
    @Operation(summary = "Get a mini-game round of the current student")
    public ResponseEntity<ApiResponse<MinigameRoundResponse>> getRound(@PathVariable Long roundId) {
        return ResponseEntity.ok(ApiResponse.success(minigameRoundService.getRound(roundId)));
    }

    @PostMapping("/rounds/{roundId}/complete")
    @Operation(summary = "Complete a mini-game round", description = "Computes the 0–100 score; the round id is the resultRefId for VOCABULARY assignments")
    public ResponseEntity<ApiResponse<MinigameRoundResponse>> completeRound(@PathVariable Long roundId) {
        return ResponseEntity.ok(ApiResponse.success(minigameRoundService.completeRound(roundId)));
    }
}

