package com.example.english_app.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2FailureHandlerTest {

    private OAuth2FailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "redirectUri", "englishapp://oauth2/redirect");
    }

    @Test
    void accessDeniedRedirectsWithSafeCancellationCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied", "provider detail", null)));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?error=access_denied");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void providerFailureDoesNotLeakProviderDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new AuthenticationServiceException("client_secret and provider details"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?error=oauth2_failed");
        assertThat(response.getRedirectedUrl()).doesNotContain("client_secret", "provider");
    }
}
