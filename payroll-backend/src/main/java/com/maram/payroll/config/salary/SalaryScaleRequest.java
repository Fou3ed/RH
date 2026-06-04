package com.maram.payroll.config.salary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryScaleRequest(
        @NotNull Long categoryId,
        @NotNull @Min(1) @Max(14) Integer echelon,
        @NotNull Integer year,
        @NotNull @Positive BigDecimal salaryMultiplier,
        BigDecimal annualSalary,
        BigDecimal increaseAmount,
        BigDecimal increasePercent,
        LocalDate validFrom,
        LocalDate validTo) {
}
