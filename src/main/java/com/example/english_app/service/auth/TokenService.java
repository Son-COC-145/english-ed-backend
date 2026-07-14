package com.example.english_app.service.auth;

import com.example.english_app.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiration))
                .subject(user.getEmail())
                .claim("type", "access")
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshTokenExpiration))
                .subject(user.getEmail())
                .claim("type", "refresh")
                .claim("userId", user.getId())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    public String getEmailFromToken(String token) {
        return jwtDecoder.decode(token).getSubject();
    }

    public Long getUserId(String token) {
        return jwtDecoder.decode(token).getClaim("userId");
    }

    public String getRole(String token) {
        return jwtDecoder.decode(token).getClaim("role");
    }

    public boolean isAccessToken(String token) {
        return "access".equals(jwtDecoder.decode(token).getClaim("type"));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(jwtDecoder.decode(token).getClaim("type"));
    }

    public boolean isTokenValid(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt() != null
                    && jwt.getExpiresAt().isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}