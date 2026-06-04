package com.maram.payroll.employee.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PositionRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull Long departmentId,
        @Min(1) @Max(14) Integer salaryGrade) {
}
