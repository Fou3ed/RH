package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Base salary = configuredBase × salaryScaleMultiplier.
 * Attendance-adjusted salary = base × (daysWorked / 26).
 */
@Component
public class SalaryCalculator {

    public BigDecimal calculateBaseSalary(BigDecimal configuredBase, BigDecimal multiplier) {
        return Money.round(Money.nz(configuredBase).multiply(Money.nz(multiplier)));
    }

    public BigDecimal adjustForAttendance(BigDecimal baseSalary, BigDecimal daysWorked) {
        return Money.round(Money.nz(baseSalary).multiply(Money.attendanceRatio(Money.nz(daysWorked))));
    }
}
