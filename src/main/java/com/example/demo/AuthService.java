package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register(String email, String rawPassword) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new EmailAlreadyExistsException("Email already registered: " + email);
        });
        User user = new User(null, email, passwordEncoder.encode(rawPassword), Role.BUYER, Instant.now());
        return userRepository.save(user);
    }

    public AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(jwtService.hashToken(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + stored.getUserId()));

        return issueTokens(user);
    }

    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(jwtService.hashToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken(
                null,
                user.getId(),
                jwtService.hashToken(rawRefreshToken),
                Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS),
                false);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, user.getId(), user.getEmail(), user.getRole());
    }
}
