package com.example.english_app.security.oauth2;

import com.example.english_app.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2ExchangeCodeService {

    static final String KEY_PREFIX = "oauth2_exchange:";

    private final StringRedisTemplate redisTemplate;
    private final OAuth2Properties properties;

    public String issue(Long userId) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + code,
                userId.toString(),
                properties.getExchangeCodeTtl());
        return code;
    }

    public Long consume(String code) {
        if (code == null || code.isBlank()) {
            throw ErrorCode.INVALID_OAUTH2_CODE.toException();
        }

        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code.trim());
        if (userId == null) {
            throw ErrorCode.INVALID_OAUTH2_CODE.toException();
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw ErrorCode.INVALID_OAUTH2_CODE.toException();
        }
    }
}
