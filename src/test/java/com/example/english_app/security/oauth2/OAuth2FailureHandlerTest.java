package com.example.english_app.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2FailureHandlerTest {

    private OAuth2FailureHandler handler;

    @BeforeEach
    void setUp() {
        OAuth2Properties properties = new OAuth2Properties();
        properties.setPostLoginUri("englishapp://oauth2/redirect");
        handler = new OAuth2FailureHandler(properties);
    }

    @Test
    void safeBusinessErrorIsReturnedToFlutter() throws Exception {
        MockHttpServletResponse response = handle("student_role_required");

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?error=student_role_required");
    }

    @Test
    void internalOAuthErrorIsNotExposed() throws Exception {
        MockHttpServletResponse response = handle("internal_provider_detail");

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?error=oauth2_authentication_failed");
    }

    private MockHttpServletResponse handle(String errorCode) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error(errorCode)));
        return response;
    }
}
