package com.example.english_app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String fullName;
    private String provider;
    private Long expiresIn;
    private String email;
    private String role;
    private Long userId;
    private String avatarUrl;

    @Schema(description = "Student onboarding summary; null when onboarding does not apply to the role")
    private Boolean onboardingCompleted;
}
