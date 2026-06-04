package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Allowance components. Attendance-adjusted allowances (presence, meal) scale by
 * daysWorked/26; fixed allowances (transport, diligence) apply in full. The child
 * allowance is per-child.
 */
@Component
public class AllowanceCalculator {

    /** A fixed or attendance-adjusted allowance, depending on {@code adjusted}. */
    public BigDecimal component(BigDecimal amount, boolean adjusted, BigDecimal daysWorked) {
        BigDecimal base = Money.nz(amount);
        if (adjusted) {
            return Money.round(base.multiply(Money.attendanceRatio(Money.nz(daysWorked))));
        }
        return Money.round(base);
    }

    /** Child allowance = per-child amount × number of children. */
    public BigDecimal child(BigDecimal perChildAmount, Integer numberOfChildren) {
        int children = numberOfChildren != null ? Math.max(0, numberOfChildren) : 0;
        return Money.round(Money.nz(perChildAmount).multiply(BigDecimal.valueOf(children)));
    }
}
