package com.example.english_app.security.oauth2;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2AccountService {

    private final UserRepository userRepository;

    @Transactional
    public User resolve(Map<String, Object> attributes) {
        String email = requiredString(attributes, "email").trim().toLowerCase(Locale.ROOT);
        String googleId = requiredString(attributes, "sub");

        if (!isVerifiedEmail(attributes)) {
            throw authenticationError("email_not_verified");
        }

        return userRepository.findByEmail(email)
                .map(user -> validateExistingStudent(user, googleId))
                .orElseGet(() -> createGoogleStudent(email, googleId, attributes));
    }

    private User validateExistingStudent(User user, String googleId) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw authenticationError("account_locked");
        }
        if (!Role.STUDENT.equals(user.getRole())) {
            throw authenticationError("student_role_required");
        }
        if (user.getProviderId() != null && !user.getProviderId().equals(googleId)) {
            throw authenticationError("provider_identity_mismatch");
        }

        // A verified Google email may be linked to an existing LOCAL student.
        if (user.getProviderId() == null) {
            user.setProviderId(googleId);
        }
        return user;
    }

    private User createGoogleStudent(
            String email,
            String googleId,
            Map<String, Object> attributes) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(optionalString(attributes, "name", email));
        user.setAvatarUrl(optionalString(attributes, "picture", null));
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(googleId);
        user.setRole(Role.STUDENT);
        user.setIsActive(true);
        user.setOnboardingCompleted(false);
        user.setPassword(null);
        return userRepository.save(user);
    }

    private boolean isVerifiedEmail(Map<String, Object> attributes) {
        Object value = attributes.containsKey("email_verified")
                ? attributes.get("email_verified")
                : attributes.get("verified_email");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String requiredString(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw authenticationError("invalid_google_profile");
        }
        return stringValue;
    }

    private String optionalString(Map<String, Object> attributes, String name, String fallback) {
        Object value = attributes.get(name);
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : fallback;
    }

    private OAuth2AuthenticationException authenticationError(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), code);
    }
}
