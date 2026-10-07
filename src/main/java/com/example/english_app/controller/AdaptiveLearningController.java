package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.adaptive.LearnerProfileResponse;
import com.example.english_app.dto.response.adaptive.TodayPlanResponse;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.adaptive.learner.LearnerProfileService;
import com.example.english_app.service.adaptive.recommendation.TodayPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Adaptive Learning", description = "Learner profile and adaptive daily plan")
public class AdaptiveLearningController {

    private final TodayPlanService todayPlanService;
    private final LearnerProfileService learnerProfileService;

    @GetMapping("/recommendations/today")
    @Operation(summary = "Get today's persisted adaptive learning plan")
    public ResponseEntity<ApiResponse<TodayPlanResponse>> today(
            Authentication authentication,
            @RequestParam(required = false) Integer budgetMinutes) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(todayPlanService.getTodayPlan(
                        userId(authentication), budgetMinutes)));
    }

    @GetMapping("/learner/profile")
    @Operation(summary = "Get the current learner profile")
    public ResponseEntity<ApiResponse<LearnerProfileResponse>> profile(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(learnerProfileService.getProfile(userId(authentication))));
    }

    private Long userId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number id = jwt.getClaim("userId");
        if (id == null) throw ErrorCode.UNAUTHORIZED.toException();
        return id.longValue();
    }
}
