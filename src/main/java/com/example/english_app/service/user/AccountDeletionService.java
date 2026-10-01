package com.example.english_app.service.user;

import com.example.english_app.dto.response.AccountDeletionResponse;
import com.example.english_app.entity.enums.AccountDeletionSource;
import com.example.english_app.entity.enums.AccountDeletionStatus;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.AccountDeletionRequest;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.notification.UserDeviceTokenRepository;
import com.example.english_app.repository.user.AccountDeletionRequestRepository;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final AccountDeletionRequestRepository requestRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${account.deletion.grace-period-hours:0}")
    private long gracePeriodHours;

    @Transactional
    public AccountDeletionResponse requestFromApp(String email, String refreshToken) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        return schedule(user, AccountDeletionSource.IN_APP, refreshToken);
    }

    @Transactional
    public AccountDeletionResponse requestFromVerifiedWeb(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        return schedule(user, AccountDeletionSource.WEB, null);
    }

    private AccountDeletionResponse schedule(
            User user,
            AccountDeletionSource source,
            String refreshToken) {
        if (!Role.STUDENT.equals(user.getRole())) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        AccountDeletionRequest existing = requestRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        LocalDateTime requestedAt = LocalDateTime.now();
        LocalDateTime availableAt = requestedAt.plusHours(Math.max(0, gracePeriodHours));
        AccountDeletionRequest request = requestRepository.save(AccountDeletionRequest.builder()
                .userId(user.getId())
                .source(source)
                .status(AccountDeletionStatus.PENDING)
                .requestedAt(requestedAt)
                .availableAt(availableAt)
                .build());

        // Security takes effect immediately; erasure itself is performed by the durable worker.
        user.setIsActive(false);
        deviceTokenRepository.deleteAllByUserId(user.getId());
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete("refresh_token:" + refreshToken);
        }

        return toResponse(request);
    }

    private AccountDeletionResponse toResponse(AccountDeletionRequest request) {
        return AccountDeletionResponse.builder()
                .requestId(request.getId())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .scheduledAt(request.getAvailableAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}
