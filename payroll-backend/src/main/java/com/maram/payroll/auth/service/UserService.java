package com.maram.payroll.auth.service;

import com.maram.payroll.auth.dto.CreateUserRequest;
import com.maram.payroll.auth.dto.UserSummary;
import com.maram.payroll.auth.entity.Role;
import com.maram.payroll.auth.entity.User;
import com.maram.payroll.auth.repository.RefreshTokenRepository;
import com.maram.payroll.auth.repository.RoleRepository;
import com.maram.payroll.auth.repository.UserRepository;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin-facing user management: create, list, role assignment, deactivation.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return UserSummary.from(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserSummary get(Long id) {
        return UserSummary.from(findUser(id));
    }

    @Transactional
    public UserSummary create(CreateUserRequest request, String actor) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmployeeId(request.employeeId());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy(actor);
        user.setRoles(resolveRoles(request.roles()));

        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public UserSummary updateRoles(Long id, Set<String> roleCodes, String actor) {
        User user = findUser(id);
        user.setRoles(resolveRoles(roleCodes));
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(actor);
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public UserSummary deactivate(Long id, String actor) {
        User user = findUser(id);
        user.setStatus("INACTIVE");
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(actor);
        // Kill any active sessions.
        refreshTokenRepository.revokeAllForUser(id);
        return UserSummary.from(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            roles.add(roleRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code)));
        }
        return roles;
    }
}
