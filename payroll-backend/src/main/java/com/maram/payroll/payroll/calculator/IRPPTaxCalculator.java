package com.maram.payroll.payroll.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Progressive IRPP income tax. Each {@link Bracket} taxes the portion of income
 * that falls within [min, max) at its rate; a tax credit (family + children) is
 * subtracted from the total, floored at zero.
 *
 * <p>Worked example (standard 2026 brackets):
 * income 6000 = (5000-2000)·10% + (6000-5000)·20% = 300 + 200 = 500.
 */
@Component
public class IRPPTaxCalculator {

    /**
     * @param min  lower bound (inclusive); null = 0
     * @param max  upper bound (exclusive); null = +∞
     * @param rate percentage (e.g. 10 for 10%)
     */
    public record Bracket(BigDecimal min, BigDecimal max, BigDecimal rate) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX = new BigDecimal("99999999");

    public BigDecimal calculate(BigDecimal taxableIncome, List<Bracket> brackets, BigDecimal taxCredit) {
        BigDecimal income = Money.nz(taxableIncome);
        BigDecimal tax = BigDecimal.ZERO;

        for (Bracket bracket : brackets) {
            BigDecimal min = bracket.min() != null ? bracket.min() : BigDecimal.ZERO;
            BigDecimal max = bracket.max() != null ? bracket.max() : MAX;
            if (income.compareTo(min) <= 0) {
                continue;
            }
            BigDecimal upper = income.min(max);
            BigDecimal inBracket = upper.subtract(min);
            if (inBracket.signum() > 0) {
                tax = tax.add(inBracket.multiply(Money.nz(bracket.rate())).divide(HUNDRED));
            }
        }

        tax = tax.subtract(Money.nz(taxCredit)).max(BigDecimal.ZERO);
        return Money.round(tax);
    }
}
