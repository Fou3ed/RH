package com.maram.payroll.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        String description,
        Long parentId) {
}
