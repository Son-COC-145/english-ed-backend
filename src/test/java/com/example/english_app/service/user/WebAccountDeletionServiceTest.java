package com.example.english_app.service.user;

import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.auth.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebAccountDeletionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private EmailService emailService;
    @Mock
    private AccountDeletionService accountDeletionService;

    private WebAccountDeletionService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new WebAccountDeletionService(
                userRepository, redisTemplate, emailService, accountDeletionService);
    }

    @Test
    void requestVerification_sendsOneTimeLinkForStudent() {
        User user = User.builder()
                .id(42L)
                .email("student@example.com")
                .fullName("Student")
                .role(Role.STUDENT)
                .build();
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        service.requestVerification(" Student@Example.com ");

        verify(valueOperations).set(anyString(), eq("42"), any(Duration.class));
        verify(emailService).sendAccountDeletionVerificationEmail(eq("student@example.com"), anyString());
    }

    @Test
    void requestVerification_doesNotRevealUnknownAccount() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestVerification("unknown@example.com");

        verify(emailService, never()).sendAccountDeletionVerificationEmail(anyString(), anyString());
    }

    @Test
    void confirm_consumesTokenAndSchedulesDeletion() {
        when(valueOperations.getAndDelete("account_deletion:web:token:token"))
                .thenReturn("42");

        service.confirm("token");

        verify(accountDeletionService).requestFromVerifiedWeb(42L);
    }
}
