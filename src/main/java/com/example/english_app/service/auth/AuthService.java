package com.example.english_app.service.auth;

import com.example.english_app.dto.request.ChangePasswordRequest;
import com.example.english_app.dto.request.LoginRequest;
import com.example.english_app.dto.request.RegisterRequest;
import com.example.english_app.dto.response.AuthResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    public UserResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(
                request.getEmail().toLowerCase().trim())
                .isPresent()) {
            throw ErrorCode.EMAIL_ALREADY_EXISTS.toException();
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(Role.STUDENT);
        user.setProvider(AuthProvider.LOCAL);
        user.setIsActive(true);

        userRepository.save(user);

        // emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return toUserResponse(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> ErrorCode.INVALID_CREDENTIALS.toException());

        if (AuthProvider.GOOGLE.equals(user.getProvider())) {
            throw ErrorCode.GOOGLE_LOGIN_RESTRICTED.toException();
        }

        if(!user.getIsActive()) {
            throw ErrorCode.ACCOUNT_LOCKED.toException();
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw  ErrorCode.INVALID_CREDENTIALS.toException();
        }

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        redisTemplate.opsForValue().set(
                "refresh_token:" + refreshToken,
                user.getEmail(),
                Duration.ofSeconds(refreshTokenExpiration));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .build();
    }

    public void forgotPassword(String email) {

        User user = userRepository
                .findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        if (AuthProvider.GOOGLE.equals(user.getProvider())) {
            throw ErrorCode.GOOGLE_PASSWORD_RESET_NOT_ALLOWED.toException();
        }

        String resetToken = java.util.UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "reset_password:" + resetToken,
                user.getEmail(),
                Duration.ofMinutes(15));

        emailService.sendResetPasswordEmail(
                user.getEmail(), resetToken);
    }

    public void resetPassword(String resetToken,
                              String newPassword) {

        String email = redisTemplate.opsForValue()
                .get("reset_password:" + resetToken);

        if (email == null) {
            throw ErrorCode.INVALID_RESET_LINK.toException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete("reset_password:" + resetToken);
    }

    public void changePassword(String email,
                               ChangePasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {
            throw ErrorCode.PASSWORD_MISMATCH.toException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        if (!passwordEncoder.matches(
                request.getOldPassword(), user.getPassword())) {
            throw ErrorCode.INCORRECT_OLD_PASSWORD.toException();
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public AuthResponse refreshToken(String refreshToken) {

        if (!tokenService.isTokenValid(refreshToken) || !tokenService.isRefreshToken(refreshToken)) {
            throw ErrorCode.INVALID_TOKEN.toException();
        }

        Boolean hasKey = redisTemplate.hasKey("refresh_token:" + refreshToken);
        if (hasKey == null || !hasKey) {
            throw ErrorCode.TOKEN_REVOKED.toException();
        }

        String email = tokenService
                .getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        String newAccessToken = tokenService
                .generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .build();
    }

    public void logout(String refreshToken) {
        if (!tokenService.isTokenValid(refreshToken)) {
            throw ErrorCode.INVALID_TOKEN.toException();
        }

        redisTemplate.delete("refresh_token:" + refreshToken);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

