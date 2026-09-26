package com.example.english_app.service.auth;

import com.example.english_app.dto.request.OAuth2ExchangeRequest;
import com.example.english_app.dto.response.AuthResponse;
import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800L);
    }

    @Test
    @DisplayName("Exchange OAuth2 code thành công -> trả về AuthResponse và lưu refreshToken")
    void exchangeOAuth2Code_success() {
        String code = "test-auth-code";
        String key = "oauth2_code:" + code;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail("google.user@example.com");
        user.setFullName("Google User");
        user.setRole(Role.STUDENT);
        user.setProvider(AuthProvider.GOOGLE);
        user.setIsActive(true);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(String.valueOf(userId));
        when(redisTemplate.delete(key)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenService.generateAccessToken(user)).thenReturn("mock-access-token");
        when(tokenService.generateRefreshToken(user)).thenReturn("mock-refresh-token");

        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code(code).build();
        AuthResponse response = authService.exchangeOAuth2Code(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getEmail()).isEqualTo("google.user@example.com");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getProvider()).isEqualTo("GOOGLE");

        verify(redisTemplate).delete(key);
        verify(valueOperations).set(eq("refresh_token:mock-refresh-token"), eq(user.getEmail()), any(Duration.class));
    }

    @Test
    @DisplayName("Exchange OAuth2 code không tồn tại hoặc đã hết hạn -> ném INVALID_OAUTH2_CODE")
    void exchangeOAuth2Code_codeNotFoundOrExpired() {
        String code = "expired-code";
        String key = "oauth2_code:" + code;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code(code).build();

        assertThatThrownBy(() -> authService.exchangeOAuth2Code(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_OAUTH2_CODE));

        verify(redisTemplate, never()).delete(anyString());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Exchange OAuth2 code bị tranh chấp đồng thời -> ném INVALID_OAUTH2_CODE")
    void exchangeOAuth2Code_concurrentReplay_fails() {
        String code = "concurrent-code";
        String key = "oauth2_code:" + code;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("10");
        when(redisTemplate.delete(key)).thenReturn(false);

        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code(code).build();

        assertThatThrownBy(() -> authService.exchangeOAuth2Code(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_OAUTH2_CODE));

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Exchange OAuth2 code với user bị khóa -> ném ACCOUNT_LOCKED")
    void exchangeOAuth2Code_userLocked() {
        String code = "valid-code-locked-user";
        String key = "oauth2_code:" + code;
        Long userId = 10L;

        User user = new User();
        user.setId(userId);
        user.setEmail("locked@example.com");
        user.setIsActive(false);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(String.valueOf(userId));
        when(redisTemplate.delete(key)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code(code).build();

        assertThatThrownBy(() -> authService.exchangeOAuth2Code(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));

        verify(tokenService, never()).generateAccessToken(any());
    }
}
