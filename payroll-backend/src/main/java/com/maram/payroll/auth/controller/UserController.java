package com.maram.payroll.auth.controller;

import com.maram.payroll.auth.dto.CreateUserRequest;
import com.maram.payroll.auth.dto.UpdateUserRolesRequest;
import com.maram.payroll.auth.dto.UserSummary;
import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin user management. Every endpoint requires the {@code user.manage} permission.
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('user.manage')")
@Tag(name = "Users", description = "Admin user & role management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserSummary> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user")
    public UserSummary get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a user and assign roles")
    public ResponseEntity<UserSummary> create(@Valid @RequestBody CreateUserRequest request) {
        String actor = SecurityUtils.getCurrentUsername().orElse("system");
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request, actor));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace a user's roles")
    public UserSummary updateRoles(@PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRolesRequest request) {
        String actor = SecurityUtils.getCurrentUsername().orElse("system");
        return userService.updateRoles(id, request.roles(), actor);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user and revoke their sessions")
    public UserSummary deactivate(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUsername().orElse("system");
        return userService.deactivate(id, actor);
    }
}
