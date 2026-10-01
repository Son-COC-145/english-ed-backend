package com.example.english_app.service.user;

import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebAccountDeletionService {

    private static final String TOKEN_PREFIX = "account_deletion:web:token:";
    private static final String COOLDOWN_PREFIX = "account_deletion:web:cooldown:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration EMAIL_COOLDOWN = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final AccountDeletionService accountDeletionService;

    /** Always returns normally so callers cannot use the endpoint to enumerate accounts. */
    public void requestVerification(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String cooldownKey = COOLDOWN_PREFIX + sha256(email);
        Boolean firstRequest = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", EMAIL_COOLDOWN);
        if (!Boolean.TRUE.equals(firstRequest)) {
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null
                || !Role.STUDENT.equals(user.getRole())
                || user.getDeletedAt() != null) {
            return;
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, user.getId().toString(), TOKEN_TTL);
        emailService.sendAccountDeletionVerificationEmail(user.getEmail(), token);
    }

    public void confirm(String rawToken) {
        String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_PREFIX + rawToken.trim());
        if (userId == null) {
            throw ErrorCode.INVALID_ACCOUNT_DELETION_TOKEN.toException();
        }
        try {
            accountDeletionService.requestFromVerifiedWeb(Long.valueOf(userId));
        } catch (NumberFormatException exception) {
            throw ErrorCode.INVALID_ACCOUNT_DELETION_TOKEN.toException();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
