package com.maram.payroll.dashboard.dto;

import java.math.BigDecimal;

/**
 * Finance KPI snapshot for the most recent payroll period. {@code periodId} is null
 * when no period exists yet.
 *
 * @param percentComplete calculated rows ÷ active employees, as a 0–100 percentage
 */
public record PayrollDashboard(
        Long periodId,
        String periodCode,
        String status,
        long activeEmployees,
        long calculated,
        long approved,
        long pendingApproval,
        BigDecimal percentComplete,
        BigDecimal totalGross,
        BigDecimal totalNet) {
}
