package com.example.demo;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtService jwtService = new JwtService("test-secret-key-at-least-32-bytes-long-1234567890", 15);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesABuyerWithAnEncodedPassword() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        User created = authService.register("new@example.com", "secret123");

        assertThat(created.getId()).isEqualTo("user-1");
        assertThat(created.getRole()).isEqualTo(Role.BUYER);
        assertThat(created.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void registerRejectsADuplicateEmail() {
        when(userRepository.findByEmail("dup@example.com"))
                .thenReturn(Optional.of(new User("user-1", "dup@example.com", "hash", Role.BUYER, Instant.now())));

        assertThatThrownBy(() -> authService.register("dup@example.com", "secret123"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginReturnsTokensOnValidCredentials() {
        User user = new User("user-1", "buyer@example.com", "hashed", Role.BUYER, Instant.now());
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login("buyer@example.com", "secret123");

        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getRole()).isEqualTo(Role.BUYER);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();

        Claims claims = jwtService.parseAndValidate(response.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo("user-1");
    }

    @Test
    void loginRejectsAnUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@example.com", "whatever"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsAWrongPassword() {
        User user = new User("user-1", "buyer@example.com", "hashed", Role.BUYER, Instant.now());
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("buyer@example.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshRotatesTheTokenAndRevokesTheOldOne() {
        User user = new User("user-1", "buyer@example.com", "hashed", Role.BUYER, Instant.now());
        RefreshToken stored = new RefreshToken(
                "rt-1", "user-1", jwtService.hashToken("raw-token"), Instant.now().plusSeconds(3600), false);

        when(refreshTokenRepository.findByTokenHash(jwtService.hashToken("raw-token")))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh("raw-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.getRefreshToken()).isNotEqualTo("raw-token");
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshRejectsARevokedToken() {
        RefreshToken revoked = new RefreshToken(
                "rt-1", "user-1", jwtService.hashToken("raw-token"), Instant.now().plusSeconds(3600), true);
        when(refreshTokenRepository.findByTokenHash(jwtService.hashToken("raw-token")))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshRejectsAnExpiredToken() {
        RefreshToken expired = new RefreshToken(
                "rt-1", "user-1", jwtService.hashToken("raw-token"), Instant.now().minusSeconds(1), false);
        when(refreshTokenRepository.findByTokenHash(jwtService.hashToken("raw-token")))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
