package com.example.english_app.service.speaking;

import com.example.english_app.annotation.RateLimitedAi;
import com.example.english_app.aspect.RateLimitAspect;
import com.example.english_app.controller.SpeakingSessionController;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.subscription.SubscriptionPlanRepository;
import com.example.english_app.repository.subscription.UserSubscriptionRepository;
import com.example.english_app.service.integration.AiRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

class SpeakingQuotaAspectTest {
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test void committedDuplicateBypassesQuotaEvenWhenNoUsageIsAvailable() throws Exception {
        var limiter = mock(AiRateLimiterService.class);
        var users = mock(UserRepository.class);
        var subscriptions = mock(UserSubscriptionRepository.class);
        var plans = mock(SubscriptionPlanRepository.class);
        var request = mock(HttpServletRequest.class);
        var identity = mock(SpeakingQuotaIdentity.class);
        var aspect = new RateLimitAspect(limiter, users, subscriptions, plans, request, identity);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fake@example.test", "", List.of()));
        when(users.findByEmail("fake@example.test")).thenReturn(Optional.of(User.builder().id(7L).build()));
        when(request.getHeader("Idempotency-Key")).thenReturn("logical-key");
        when(request.getRequestURI()).thenReturn("/api/v1/speaking-session/start");
        when(identity.committed(7L, "/api/v1/speaking-session/start", "logical-key")).thenReturn(true);
        RateLimitedAi annotation = SpeakingSessionController.class.getMethod("start", String.class,
                com.example.english_app.dto.request.StartSessionRequest.class).getAnnotation(RateLimitedAi.class);
        aspect.checkRateLimit(mock(JoinPoint.class), annotation);
        verifyNoInteractions(limiter, subscriptions, plans);
    }
}
