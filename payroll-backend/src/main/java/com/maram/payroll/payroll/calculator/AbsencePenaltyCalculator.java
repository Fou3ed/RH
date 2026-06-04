package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Absence penalty = daysAbsent × (baseSalary / 26).
 *
 * <p>NOTE: the default {@code PayrollCalculationService} prorates the base salary by
 * attendance (base × daysWorked/26), which already accounts for unpaid absences, so
 * it does not also apply this penalty (that would double-count). This calculator is
 * provided for an alternative, non-prorated policy and is unit-tested in isolation.
 */
@Component
public class AbsencePenaltyCalculator {

    public BigDecimal calculate(BigDecimal baseSalary, BigDecimal daysAbsent) {
        // The daily rate is itself a money amount, rounded to 2 decimals before
        // being multiplied by the number of absent days (matches the payslip method).
        BigDecimal dailyRate = Money.round(
                Money.nz(baseSalary).divide(Money.WORKING_DAYS, Money.RATIO_SCALE, java.math.RoundingMode.HALF_UP));
        return Money.round(dailyRate.multiply(Money.nz(daysAbsent)));
    }
}
