package com.maram.payroll.auth.dto;

/**
 * Returned by {@code POST /auth/login}. {@code expiresIn} is the access-token
 * lifetime in seconds.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        long expiresIn,
        UserSummary user) {
}
