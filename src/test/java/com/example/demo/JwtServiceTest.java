package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-1234567890";

    private final JwtService jwtService = new JwtService(SECRET, 15);

    private User buyer() {
        return new User("user-1", "buyer@example.com", "hash", Role.BUYER, Instant.now());
    }

    @Test
    void generatesAndParsesAValidAccessToken() {
        String token = jwtService.generateAccessToken(buyer());

        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("role", String.class)).isEqualTo("BUYER");
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService expiredTokenIssuer = new JwtService(SECRET, -1);
        String token = expiredTokenIssuer.generateAccessToken(buyer());

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-value-1234567890", 15);
        String token = otherService.generateAccessToken(buyer());

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refreshTokenValuesAreRandomAndHashingIsDeterministic() {
        String raw = jwtService.generateRefreshTokenValue();
        String other = jwtService.generateRefreshTokenValue();

        assertThat(raw).isNotEqualTo(other);
        assertThat(jwtService.hashToken(raw)).isEqualTo(jwtService.hashToken(raw));
        assertThat(jwtService.hashToken(raw)).isNotEqualTo(jwtService.hashToken(other));
    }
}
