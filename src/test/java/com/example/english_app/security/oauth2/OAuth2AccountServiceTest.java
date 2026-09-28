package com.example.english_app.security.oauth2;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AccountServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private OAuth2AccountService service;

    @Test
    void newGoogleAccountIsAlwaysAnActiveStudent() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.resolve(profile("Student@Example.com", "google-1", true));

        assertThat(user.getEmail()).isEqualTo("student@example.com");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getPassword()).isNull();
        assertThat(user.getOnboardingCompleted()).isFalse();
    }

    @Test
    void existingTeacherCannotLoginWithGoogle() {
        User teacher = existingUser(Role.TEACHER, AuthProvider.LOCAL);
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));

        assertOauthError(
                () -> service.resolve(profile(teacher.getEmail(), "google-1", true)),
                "student_role_required");
        verify(userRepository, never()).save(any());
    }

    @Test
    void existingAdminCannotLoginWithGoogle() {
        User admin = existingUser(Role.ADMIN, AuthProvider.LOCAL);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        assertOauthError(
                () -> service.resolve(profile(admin.getEmail(), "google-1", true)),
                "student_role_required");
    }

    @Test
    void verifiedGoogleIdentityCanBeLinkedToExistingLocalStudent() {
        User student = existingUser(Role.STUDENT, AuthProvider.LOCAL);
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        User resolved = service.resolve(profile(student.getEmail(), "google-1", true));

        assertThat(resolved).isSameAs(student);
        assertThat(resolved.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(resolved.getProviderId()).isEqualTo("google-1");
    }

    @Test
    void unverifiedEmailIsRejectedBeforeAccountLookup() {
        assertOauthError(
                () -> service.resolve(profile("student@example.com", "google-1", false)),
                "email_not_verified");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void providerIdentityMismatchIsRejected() {
        User student = existingUser(Role.STUDENT, AuthProvider.GOOGLE);
        student.setProviderId("google-old");
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        assertOauthError(
                () -> service.resolve(profile(student.getEmail(), "google-new", true)),
                "provider_identity_mismatch");
    }

    private User existingUser(Role role, AuthProvider provider) {
        return User.builder()
                .id(1L)
                .email("student@example.com")
                .fullName("User")
                .role(role)
                .provider(provider)
                .isActive(true)
                .build();
    }

    private Map<String, Object> profile(String email, String sub, boolean verified) {
        return Map.of(
                "email", email,
                "sub", sub,
                "email_verified", verified,
                "name", "Student");
    }

    private void assertOauthError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            String expectedCode) {
        assertThatThrownBy(action)
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error)
                        .getError().getErrorCode()).isEqualTo(expectedCode));
    }
}
