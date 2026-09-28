package com.example.english_app.service.auth;

import com.example.english_app.dto.request.LoginRequest;
import com.example.english_app.dto.request.OAuth2ExchangeRequest;
import com.example.english_app.dto.response.AuthResponse;
import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.security.oauth2.OAuth2ExchangeCodeService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private OAuth2ExchangeCodeService exchangeCodeService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800L);
    }

    @Test
    void localStudentLoginReturnsPersistedOnboardingSummary() {
        User student = student(AuthProvider.LOCAL);
        student.setOnboardingCompleted(true);
        LoginRequest request = new LoginRequest();
        request.setEmail(student.getEmail());
        request.setPassword("Password1!");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(request.getPassword(), student.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(student)).thenReturn("access");
        when(tokenService.generateRefreshToken(student)).thenReturn("refresh");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(request);

        assertThat(response.getOnboardingCompleted()).isTrue();
        assertThat(response.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void localStaffLoginDoesNotExposeOnboardingState() {
        User teacher = student(AuthProvider.LOCAL);
        teacher.setRole(Role.TEACHER);
        LoginRequest request = new LoginRequest();
        request.setEmail(teacher.getEmail());
        request.setPassword("Password1!");

        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(request.getPassword(), teacher.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(teacher)).thenReturn("access");
        when(tokenService.generateRefreshToken(teacher)).thenReturn("refresh");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(request);

        assertThat(response.getOnboardingCompleted()).isNull();
    }

    @Test
    void exchangeOAuth2CodeIssuesTokensForActiveStudent() {
        User student = student(AuthProvider.GOOGLE);
        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code("one-time-code").build();

        when(exchangeCodeService.consume("one-time-code")).thenReturn(student.getId());
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(tokenService.generateAccessToken(student)).thenReturn("access");
        when(tokenService.generateRefreshToken(student)).thenReturn("refresh");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.exchangeOAuth2Code(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getOnboardingCompleted()).isFalse();
        verify(valueOperations).set(
                eq("refresh_token:refresh"),
                eq(student.getEmail()),
                any(Duration.class));
    }

    @Test
    void exchangeOAuth2CodeRejectsStaffEvenIfAValidCodeExists() {
        User teacher = student(AuthProvider.LOCAL);
        teacher.setRole(Role.TEACHER);
        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code("staff-code").build();

        when(exchangeCodeService.consume("staff-code")).thenReturn(teacher.getId());
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> authService.exchangeOAuth2Code(request))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));

        verify(tokenService, never()).generateAccessToken(any());
    }

    @Test
    void exchangeOAuth2CodeRejectsLockedStudent() {
        User student = student(AuthProvider.GOOGLE);
        student.setIsActive(false);
        OAuth2ExchangeRequest request = OAuth2ExchangeRequest.builder().code("locked-code").build();

        when(exchangeCodeService.consume("locked-code")).thenReturn(student.getId());
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.exchangeOAuth2Code(request))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    private User student(AuthProvider provider) {
        return User.builder()
                .id(10L)
                .email("student@example.com")
                .password("encoded")
                .fullName("Student")
                .role(Role.STUDENT)
                .provider(provider)
                .isActive(true)
                .onboardingCompleted(false)
                .build();
    }
}
