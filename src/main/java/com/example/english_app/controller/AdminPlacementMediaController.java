package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.service.onboarding.PlacementListeningMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/questions/listening-media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Placement Media", description = "Pre-generate listening audio for placement questions")
public class AdminPlacementMediaController {

    private final PlacementListeningMediaService mediaService;

    @Operation(summary = "Generate missing listening MP3 files and upload them to Azure Blob Storage")
    @PostMapping("/generate-missing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMissing() {
        return ResponseEntity.ok(ApiResponse.success(mediaService.generateMissingAudio()));
    }
}
