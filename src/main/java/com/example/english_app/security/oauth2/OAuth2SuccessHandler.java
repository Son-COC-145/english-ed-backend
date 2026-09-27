package com.example.english_app.security.oauth2;

import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2ExchangeCodeService exchangeCodeService;
    private final OAuth2Properties properties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof CustomOAuth2User oauthUser)) {
            clearServerSideSession(request);
            redirect(request, response, "error", "oauth2_authentication_failed");
            return;
        }

        User user = oauthUser.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            clearServerSideSession(request);
            redirect(request, response, "error", "account_locked");
            return;
        }
        if (!Role.STUDENT.equals(user.getRole())) {
            clearServerSideSession(request);
            redirect(request, response, "error", "student_role_required");
            return;
        }

        try {
            String oneTimeCode = exchangeCodeService.issue(user.getId());
            log.info("Google OAuth2 login succeeded for student userId={}", user.getId());
            clearServerSideSession(request);
            redirect(request, response, "code", oneTimeCode);
        } catch (RuntimeException exception) {
            log.error("Could not issue OAuth2 exchange code for userId={}", user.getId(), exception);
            clearServerSideSession(request);
            redirect(request, response, "error", "exchange_code_unavailable");
        }
    }

    private void clearServerSideSession(HttpServletRequest request) {
        clearAuthenticationAttributes(request);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String parameter,
            String value) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(properties.getPostLoginUri())
                .queryParam(parameter, value)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
