package com.example.english_app.security.oauth2;

import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2ExchangeCodeServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private OAuth2ExchangeCodeService service;

    @BeforeEach
    void setUp() {
        OAuth2Properties properties = new OAuth2Properties();
        properties.setPostLoginUri("englishapp://oauth2/redirect");
        properties.setExchangeCodeTtl(Duration.ofMinutes(2));
        service = new OAuth2ExchangeCodeService(redisTemplate, properties);
    }

    @Test
    void issueStoresOpaqueCodeWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String code = service.issue(42L);

        assertThat(code).isNotBlank();
        verify(valueOperations).set(
                eq(OAuth2ExchangeCodeService.KEY_PREFIX + code),
                eq("42"),
                eq(Duration.ofMinutes(2)));
    }

    @Test
    void consumeAtomicallyDeletesAndReturnsUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(OAuth2ExchangeCodeService.KEY_PREFIX + "code"))
                .thenReturn("42");

        assertThat(service.consume("code")).isEqualTo(42L);
    }

    @Test
    void consumedOrExpiredCodeIsRejected() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertInvalidCode(() -> service.consume("missing"));
    }

    @Test
    void blankCodeIsRejectedWithoutRedisCall() {
        assertInvalidCode(() -> service.consume("  "));
        verify(redisTemplate, never()).opsForValue();
    }

    private void assertInvalidCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_OAUTH2_CODE));
    }
}
