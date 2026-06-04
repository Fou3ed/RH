package com.maram.payroll.auth.dto;

import com.maram.payroll.auth.entity.User;
import com.maram.payroll.auth.security.CustomUserDetails;

import java.util.List;
import java.util.Set;

/**
 * Public projection of a user (never exposes the password hash).
 */
public record UserSummary(
        Long id,
        String username,
        String email,
        Long employeeId,
        String status,
        Set<String> roles,
        Set<String> permissions) {

    public static UserSummary from(CustomUserDetails details) {
        return new UserSummary(
                details.getId(),
                details.getUsername(),
                details.getUser().getEmail(),
                details.getUser().getEmployeeId(),
                details.getUser().getStatus(),
                details.getRoleCodes(),
                details.getPermissionCodes());
    }

    public static UserSummary from(User user) {
        return UserSummary.from(new CustomUserDetails(user));
    }

    public static List<UserSummary> from(List<User> users) {
        return users.stream().map(UserSummary::from).toList();
    }
}
