package com.example.english_app.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final String ACCESS_DENIED = "access_denied";
    private static final String DEFAULT_ERROR = "oauth2_failed";
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Value("${spring.security.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String error = DEFAULT_ERROR;
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String candidate = oauth2Exception.getError().getErrorCode();
            if (ACCESS_DENIED.equals(candidate)) {
                error = ACCESS_DENIED;
            }
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.setHeader("Cache-Control", "no-store");

        String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", error)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }
}
