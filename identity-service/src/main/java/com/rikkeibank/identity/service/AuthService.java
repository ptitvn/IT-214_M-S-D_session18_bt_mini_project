package com.rikkeibank.identity.service;

import com.rikkeibank.identity.dto.AuthDtos.*;
import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.exception.EntityNotFoundException;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles login / refresh / logout / force-logout.
 *
 * Seamless session experience for CUSTOMER (per SRS): access token is short-lived (15 min) but
 * the refresh token is long-lived (30 days) and stored server-side, so the mobile app can silently
 * exchange it for a new access token without asking the user to log in again.
 *
 * ADMIN force logout (thu hồi quyền truy cập ngay lập tức): writes a blacklist marker to Redis with
 * the current timestamp. The API Gateway rejects any token whose issuedAt is older than that marker,
 * which makes revocation effectively immediate even though JWTs are stateless.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .referenceId(request.referenceId())
                .enabled(true)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("This account has been disabled by ADMIN");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        User user = userRepository.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired, please login again");
        }
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        return new TokenResponse(newAccessToken, user.getRefreshToken(), "Bearer",
                jwtTokenProvider.getAccessTokenExpirationMs(), user.getUsername(), user.getRole(), user.getReferenceId());
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
    }

    /** ADMIN-only: immediately revoke a user's access, per the SRS security requirement. */
    @Transactional
    public void forceLogout(String targetUsername) {
        User user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + targetUsername));
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);

        long now = System.currentTimeMillis();
        redisTemplate.opsForValue().set("blacklist:" + targetUsername, String.valueOf(now),
                refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now(ZoneOffset.UTC)
                .plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer",
                jwtTokenProvider.getAccessTokenExpirationMs(), user.getUsername(), user.getRole(), user.getReferenceId());
    }
}
