package com.maram.payroll.auth.security;

import com.maram.payroll.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapts a {@link User} to Spring Security. Authorities are the union of:
 * <ul>
 *   <li>role codes prefixed with {@code ROLE_} (e.g. {@code ROLE_ADMIN})</li>
 *   <li>permission codes verbatim (e.g. {@code payroll.approve})</li>
 * </ul>
 */
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private static List<GrantedAuthority> buildAuthorities(User user) {
        Stream<String> roleAuthorities = user.getRoles().stream()
                .map(r -> "ROLE_" + r.getCode());
        Stream<String> permissionAuthorities = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode());
        return Stream.concat(roleAuthorities, permissionAuthorities)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public Set<String> getRoleCodes() {
        return user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet());
    }

    public Set<String> getPermissionCodes() {
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equalsIgnoreCase(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
