package com.maram.payroll.auth.service;

import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.RefreshResponse;
import com.maram.payroll.auth.dto.UserSummary;
import com.maram.payroll.auth.entity.RefreshToken;
import com.maram.payroll.auth.entity.User;
import com.maram.payroll.auth.repository.RefreshTokenRepository;
import com.maram.payroll.auth.repository.UserRepository;
import com.maram.payroll.auth.security.CustomUserDetails;
import com.maram.payroll.auth.security.JwtTokenProvider;
import com.maram.payroll.config.JwtProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Login / refresh / logout. Access tokens are short-lived JWTs; refresh tokens are
 * opaque, persisted, and rotated on every refresh so they can be revoked at logout.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenRepository refreshTokenRepository,
                       UserRepository userRepository,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationMs = jwtProperties.refreshExpirationMs();
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        userRepository.findById(principal.getId()).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            u.setFailedLoginAttempts(0);
        });

        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(principal.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                tokenProvider.getAccessExpirationSeconds(),
                UserSummary.from(principal));
    }

    @Transactional
    public RefreshResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .filter(RefreshToken::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        // Rotate: revoke the presented token and issue a new one.
        stored.setRevoked(true);

        User user = userRepository.findById(stored.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("User no longer active"));

        CustomUserDetails principal = new CustomUserDetails(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String newRefresh = createRefreshToken(user.getId());

        return new RefreshResponse(accessToken, newRefresh, tokenProvider.getAccessExpirationSeconds());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(t -> t.setRevoked(true));
    }

    private String createRefreshToken(Long userId) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setRevoked(false);
        token.setCreatedAt(Instant.now());
        return refreshTokenRepository.save(token).getToken();
    }
}
