package com.example.english_app.security.oauth2;

import com.example.english_app.entity.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final long CODE_EXPIRATION_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.security.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        if (!user.getIsActive()) {
            String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "account_locked")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "oauth2_code:" + code,
                String.valueOf(user.getId()),
                Duration.ofSeconds(CODE_EXPIRATION_SECONDS)
        );

        String redirectUrl = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("code", code)
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}