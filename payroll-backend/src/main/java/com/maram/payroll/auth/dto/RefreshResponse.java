package com.maram.payroll.auth.dto;

/**
 * Returned by {@code POST /auth/refresh}. A fresh refresh token is issued too
 * (rotation), so the client should replace its stored value.
 */
public record RefreshResponse(
        String token,
        String refreshToken,
        long expiresIn) {
}
