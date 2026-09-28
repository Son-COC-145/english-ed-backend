package com.example.english_app.security.oauth2;

import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private OAuth2ExchangeCodeService exchangeCodeService;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(exchangeCodeService);
        ReflectionTestUtils.setField(handler, "redirectUri", "englishapp://oauth2/redirect");
    }

    @Test
    void activeStudentReceivesOnlyAnOpaqueCode() throws Exception {
        User student = user(Role.STUDENT, true);
        when(exchangeCodeService.issue(1L)).thenReturn("opaque-code");
        MockHttpServletResponse response = authenticate(student);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?code=opaque-code");
    }

    @Test
    void activeStaffAccountKeepsTheOriginalOAuthBehavior() throws Exception {
        User teacher = user(Role.TEACHER, true);
        when(exchangeCodeService.issue(teacher.getId())).thenReturn("staff-code");
        MockHttpServletResponse response = authenticate(teacher);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("englishapp://oauth2/redirect?code=staff-code");
    }

    private MockHttpServletResponse authenticate(User user) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        CustomOAuth2User principal = new CustomOAuth2User(user, Map.of());
        handler.onAuthenticationSuccess(
                request,
                response,
                new TestingAuthenticationToken(principal, null));
        return response;
    }

    private User user(Role role, boolean active) {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User")
                .role(role)
                .isActive(active)
                .build();
    }
}
