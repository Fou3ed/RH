package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Performance bonus = base salary × rate, where the rate is driven by the monthly
 * rating: 8 (excellent) → 5%, 7 (good) → 2.5%, anything else / missing → 0%.
 */
@Component
public class PerformanceBonusCalculator {

    private static final BigDecimal EXCELLENT = new BigDecimal("0.05");
    private static final BigDecimal GOOD = new BigDecimal("0.025");

    public BigDecimal calculate(BigDecimal baseSalary, Integer ratingScore) {
        BigDecimal rate = rateFor(ratingScore);
        return Money.round(Money.nz(baseSalary).multiply(rate));
    }

    private BigDecimal rateFor(Integer ratingScore) {
        if (ratingScore == null) {
            return BigDecimal.ZERO;
        }
        return switch (ratingScore) {
            case 8 -> EXCELLENT;
            case 7 -> GOOD;
            default -> BigDecimal.ZERO;
        };
    }
}
