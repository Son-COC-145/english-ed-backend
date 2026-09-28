package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.service.onboarding.OnboardingLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Onboarding", description = "Administrative onboarding operations")
public class AdminOnboardingController {

    private final OnboardingLifecycleService lifecycleService;

    @Operation(summary = "Reset onboarding for a student")
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetOnboarding(@RequestParam Long targetUserId) {
        lifecycleService.resetOnboarding(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(
                "Reset onboarding cho userId=" + targetUserId + " thành công."));
    }
}
