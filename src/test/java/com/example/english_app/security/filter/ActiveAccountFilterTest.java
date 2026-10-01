package com.example.english_app.security.filter;

import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveAccountFilterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeAccountContinuesFilterChain() throws Exception {
        authenticate(42L);
        when(userRepository.existsByIdAndIsActiveTrue(42L)).thenReturn(true);
        ActiveAccountFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void inactiveAccountIsRejectedImmediately() throws Exception {
        authenticate(42L);
        when(userRepository.existsByIdAndIsActiveTrue(42L)).thenReturn(false);
        ActiveAccountFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":1002");
        verify(filterChain, never()).doFilter(request, response);
    }

    private ActiveAccountFilter filter() {
        return new ActiveAccountFilter(userRepository, new ObjectMapper().findAndRegisterModules());
    }

    private void authenticate(Long userId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("student@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("userId", userId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }
}
