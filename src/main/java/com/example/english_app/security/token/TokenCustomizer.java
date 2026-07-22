package com.example.english_app.security.token;

import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserRepository userRepository;

    @Override
    public void customize(JwtEncodingContext context) {
        String email = context.getPrincipal().getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            context.getClaims()
                    .claim("role", user.getRole())
                    .claim("userId",user.getId())
                    .claim("provider", user.getProvider().name());
        });
    }
}

