package com.maram.payroll.attendance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Update an existing attendance record (the employee + date are fixed by its id).
 */
public record AttendanceUpdateRequest(
        @NotNull String status,
        @DecimalMin("0.0") @DecimalMax("16.0") BigDecimal hoursWorked,
        String notes,
        String absenceReason) {
}
