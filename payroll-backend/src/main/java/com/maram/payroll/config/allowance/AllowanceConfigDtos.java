package com.maram.payroll.config.allowance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs for allowance configuration (grouped to keep the package tidy). */
public final class AllowanceConfigDtos {

    private AllowanceConfigDtos() {
    }

    public record Response(
            Long id,
            String allowanceType,
            BigDecimal amount,
            boolean attendanceAdjusted,
            LocalDate effectiveDate,
            LocalDate endDate) {

        public static Response from(AllowanceConfig c) {
            return new Response(c.getId(), c.getAllowanceType(), c.getAmount(),
                    c.isAttendanceAdjusted(), c.getEffectiveDate(), c.getEndDate());
        }
    }

    public record Request(
            @NotBlank String allowanceType,
            @NotNull @PositiveOrZero BigDecimal amount,
            boolean attendanceAdjusted,
            LocalDate effectiveDate,
            LocalDate endDate) {
    }
}
