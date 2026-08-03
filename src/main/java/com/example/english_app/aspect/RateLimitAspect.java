package com.example.english_app.aspect;

import java.time.LocalDateTime;
import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.english_app.annotation.RateLimitedAi;
import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.entity.enums.SubscriptionStatus;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import com.example.english_app.entity.subscription.UserSubscription;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.subscription.SubscriptionPlanRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.subscription.UserSubscriptionRepository;
import com.example.english_app.service.integration.AiRateLimiterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {
    private final AiRateLimiterService rateLimiterService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    @Before("@annotation(rateLimitedAi)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimitedAi rateLimitedAi) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Optional<UserSubscription> activeSubscription = userSubscriptionRepository
                .findFirstByUserAndStatusAndEndDateAfterOrderByEndDateDesc(
                        user,
                        SubscriptionStatus.ACTIVE,
                        LocalDateTime.now());

        int limit = 0;
        if (activeSubscription.isPresent()) {
            SubscriptionPlan plan = activeSubscription.get().getPlan();

            if (plan.getName() == PlanName.PREMIUM) {
                log.info("User {} is PREMIUM, bypassing AI rate limit", email);
                return;
            }

            limit = plan.getAiPromptLimit() != null ? plan.getAiPromptLimit() : 10;
        } else {
            SubscriptionPlan basicPlan = planRepository.findByName(PlanName.BASIC)
                    .orElseThrow(() -> new RuntimeException("Basic plan not configured in DB"));

            limit = basicPlan.getAiPromptLimit() != null ? basicPlan.getAiPromptLimit() : 10;
        }

        boolean isAllowed = rateLimiterService.checkAndIncrementUsage(email, limit);

        if (!isAllowed) {
            log.warn("User {} exceeded AI quota limit of {}", email, limit);
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }

    }
}

