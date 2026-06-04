package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * CNSS social-security contribution = gross × rate (default 5.95%).
 */
@Component
public class CNSSCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** @param ratePercent contribution rate as a percentage, e.g. 5.95 */
    public BigDecimal calculate(BigDecimal gross, BigDecimal ratePercent) {
        return Money.round(Money.nz(gross).multiply(Money.nz(ratePercent)).divide(HUNDRED));
    }
}
