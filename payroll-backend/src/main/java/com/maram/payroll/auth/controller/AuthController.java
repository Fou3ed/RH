package com.maram.payroll.auth.controller;

import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.LoginRequest;
import com.maram.payroll.auth.dto.RefreshRequest;
import com.maram.payroll.auth.dto.RefreshResponse;
import com.maram.payroll.auth.dto.UserSummary;
import com.maram.payroll.auth.security.CustomUserDetails;
import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.auth.service.AuthService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain access + refresh tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token (rotates the refresh token)")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user")
    public UserSummary me() {
        CustomUserDetails current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user"));
        return UserSummary.from(current);
    }
}
