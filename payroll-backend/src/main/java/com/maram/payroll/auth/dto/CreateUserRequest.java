package com.maram.payroll.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        Long employeeId,
        @NotEmpty Set<String> roles) {
}
