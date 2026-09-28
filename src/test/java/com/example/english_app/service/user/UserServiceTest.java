package com.example.english_app.service.user;

import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("deleteAccount: Soft delete thành công và xóa refresh token trong Redis khi có token")
    void deleteAccount_Success_WithRefreshToken() {
        String email = "student@example.com";
        String refreshToken = "sample-refresh-token";

        User user = new User();
        user.setEmail(email);
        user.setIsActive(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userService.deleteAccount(email, refreshToken);

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
        verify(redisTemplate).delete("refresh_token:" + refreshToken);
    }

    @Test
    @DisplayName("deleteAccount: Soft delete thành công mà không gọi Redis khi refreshToken null hoặc rỗng")
    void deleteAccount_Success_WithoutRefreshToken() {
        String email = "student@example.com";

        User user = new User();
        user.setEmail(email);
        user.setIsActive(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userService.deleteAccount(email, null);

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("deleteAccount: Quăng USER_NOT_FOUND khi không tìm thấy email")
    void deleteAccount_UserNotFound_ThrowsException() {
        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAccount(email, "some-token"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(userRepository, never()).save(any());
        verify(redisTemplate, never()).delete(any(String.class));
    }
}
