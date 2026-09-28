package com.example.english_app.controller;

import com.example.english_app.dto.response.AuthResponse;
import com.example.english_app.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerOAuth2ContractTest {

    @Mock private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void exchangeUsesJsonAndReturnsOnlyTheFinalAuthenticationContract() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .email("student@example.com")
                .role("STUDENT")
                .provider("GOOGLE")
                .onboardingCompleted(false)
                .build();
        when(authService.exchangeOAuth2Code(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"one-time-code\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.data.nextStep").doesNotExist())
                .andExpect(jsonPath("$.data.loginClient").doesNotExist());
    }
}
