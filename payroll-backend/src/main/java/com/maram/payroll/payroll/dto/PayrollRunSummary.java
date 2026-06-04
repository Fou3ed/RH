package com.maram.payroll.payroll.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of a payroll run for a period.
 *
 * @param skipped employee codes that could not be calculated (missing base salary
 *                or salary scale), with the reason
 */
public record PayrollRunSummary(
        Long periodId,
        String periodCode,
        int calculated,
        int skippedCount,
        BigDecimal totalGross,
        BigDecimal totalDeductions,
        BigDecimal totalNet,
        List<String> skipped) {
}
