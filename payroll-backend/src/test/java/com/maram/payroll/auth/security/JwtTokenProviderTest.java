package com.maram.payroll.auth.security;

import com.maram.payroll.auth.entity.Permission;
import com.maram.payroll.auth.entity.Role;
import com.maram.payroll.auth.entity.User;
import com.maram.payroll.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for token generation/validation — no Spring context.
 */
class JwtTokenProviderTest {

    private final JwtProperties properties = new JwtProperties(
            "test-secret-that-is-definitely-long-enough-32+chars", 900_000L, 604_800_000L);
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    private CustomUserDetails sampleUser() {
        Permission perm = new Permission();
        perm.setCode("payroll.approve");

        Role role = new Role();
        role.setCode("FINANCE_MANAGER");
        role.setPermissions(Set.of(perm));

        User user = new User();
        user.setId(42L);
        user.setUsername("sonia.chafai");
        user.setEmail("sonia@maram.tn");
        user.setPasswordHash("irrelevant");
        user.setRoles(Set.of(role));

        return new CustomUserDetails(user);
    }

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        String token = provider.generateAccessToken(sampleUser());

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("sonia.chafai");
        assertThat(provider.getAccessExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = provider.generateAccessToken(sampleUser());
        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThat(provider.isValid(tampered)).isFalse();
        assertThat(provider.getUsername(tampered)).isNull();
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(provider.isValid("not-a-jwt")).isFalse();
    }
}
