package com.example.english_app.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Set<String> PUBLIC_ERROR_CODES = Set.of(
            "account_locked",
            "email_not_verified",
            "student_role_required",
            "provider_identity_mismatch",
            "invalid_google_profile");

    private final OAuth2Properties properties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String errorCode = publicErrorCode(exception);
        log.warn("Google OAuth2 login failed: error={}", errorCode);
        clearServerSideSession(request);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(properties.getPostLoginUri())
                .queryParam("error", errorCode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void clearServerSideSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String publicErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String code = oauthException.getError().getErrorCode();
            if (PUBLIC_ERROR_CODES.contains(code)) {
                return code;
            }
        }
        return "oauth2_authentication_failed";
    }
}
