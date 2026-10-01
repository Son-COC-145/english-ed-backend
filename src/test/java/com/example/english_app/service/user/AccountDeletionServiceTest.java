package com.example.english_app.service.user;

import com.example.english_app.dto.response.AccountDeletionResponse;
import com.example.english_app.entity.enums.AccountDeletionStatus;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.AccountDeletionRequest;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.notification.UserDeviceTokenRepository;
import com.example.english_app.repository.user.AccountDeletionRequestRepository;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountDeletionRequestRepository requestRepository;
    @Mock
    private UserDeviceTokenRepository deviceTokenRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AccountDeletionService service;

    @Test
    void requestFromApp_disablesAccountAndEnqueuesDurableRequest() {
        User user = student(42L, true);
        when(userRepository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(requestRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(requestRepository.save(any(AccountDeletionRequest.class))).thenAnswer(invocation -> {
            AccountDeletionRequest request = invocation.getArgument(0);
            request.setId(7L);
            return request;
        });

        AccountDeletionResponse response = service.requestFromApp(user.getEmail(), "refresh-token");

        assertThat(user.getIsActive()).isFalse();
        assertThat(response.getRequestId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(response.getRequestedAt()).isNotNull();
        verify(deviceTokenRepository).deleteAllByUserId(42L);
        verify(redisTemplate).delete("refresh_token:refresh-token");
    }

    @Test
    void requestFromApp_isIdempotentWhenRequestAlreadyExists() {
        User user = student(42L, false);
        AccountDeletionRequest existing = AccountDeletionRequest.builder()
                .id(7L)
                .userId(42L)
                .status(AccountDeletionStatus.PROCESSING)
                .availableAt(LocalDateTime.now())
                .build();
        when(userRepository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(requestRepository.findByUserId(42L)).thenReturn(Optional.of(existing));

        AccountDeletionResponse response = service.requestFromApp(user.getEmail(), null);

        assertThat(response.getRequestId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(AccountDeletionStatus.PROCESSING);
        verify(requestRepository, never()).save(any());
        verify(deviceTokenRepository, never()).deleteAllByUserId(42L);
    }

    @Test
    void requestFromApp_rejectsManagedTeacherAccount() {
        User teacher = student(9L, true);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByEmailForUpdate(teacher.getEmail())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.requestFromApp(teacher.getEmail(), null))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    private User student(Long id, boolean active) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .fullName("Student")
                .role(Role.STUDENT)
                .isActive(active)
                .build();
    }
}
