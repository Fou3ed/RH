package com.maram.payroll.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code maram.jwt.*}.
 *
 * @param secret              HMAC signing secret (≥ 32 chars for HS256)
 * @param accessExpirationMs  access-token lifetime in milliseconds
 * @param refreshExpirationMs refresh-token lifetime in milliseconds
 */
@ConfigurationProperties(prefix = "maram.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationMs,
        long refreshExpirationMs) {
}
