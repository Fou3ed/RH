package com.maram.payroll.attendance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Record/update a single day's attendance. {@code status} must be one of the
 * {@code AttendanceStatus} names; hours are bounded 0–16.
 */
public record AttendanceRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate attendanceDate,
        @NotNull String status,
        @DecimalMin("0.0") @DecimalMax("16.0") BigDecimal hoursWorked,
        String notes,
        String absenceReason) {
}
