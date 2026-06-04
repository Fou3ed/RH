package com.maram.payroll.auth.security;

import com.maram.payroll.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates HS256 access tokens. Refresh tokens are opaque and stored
 * server-side (see {@code RefreshToken}); only access tokens are JWTs.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ISSUER = "payroll.maram.tn";

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = properties.accessExpirationMs();
    }

    /** Builds a signed access token carrying identity + RBAC claims. */
    public String generateAccessToken(CustomUserDetails principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(principal.getId()))
                .claim("username", principal.getUsername())
                .claim("email", principal.getUser().getEmail())
                .claim("roles", List.copyOf(principal.getRoleCodes()))
                .claim("permissions", List.copyOf(principal.getPermissionCodes()))
                .claim("scope", "api")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /** @return the username claim, or {@code null} if the token is invalid/expired. */
    public String getUsername(String token) {
        try {
            return parse(token).get("username", String.class);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
