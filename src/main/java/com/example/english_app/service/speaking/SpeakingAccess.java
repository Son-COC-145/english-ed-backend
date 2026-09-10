package com.example.english_app.service.speaking;

import org.springframework.security.authentication.AnonymousAuthenticationToken;

import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpeakingAccess {

    private final UserRepository users;

    public Long userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw ErrorCode.UNAUTHORIZED.toException();
        }
        return users.findByEmail(auth.getName())
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException)
                .getId();
    }

    public static boolean managesScenarios() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_TEACHER".equals(a.getAuthority()));
    }
}
