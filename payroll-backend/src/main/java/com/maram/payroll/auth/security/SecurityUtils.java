package com.maram.payroll.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Convenience accessors for the currently authenticated principal.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CustomUserDetails> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(CustomUserDetails::getUsername);
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(CustomUserDetails::getId);
    }
}
