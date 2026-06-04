package com.maram.payroll.payroll.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers. All payroll amounts are rounded to 2 decimals HALF_UP; ratios
 * are computed at higher precision to avoid compounding rounding error.
 */
public final class Money {

    public static final int MONEY_SCALE = 2;
    public static final int RATIO_SCALE = 10;
    public static final BigDecimal WORKING_DAYS = new BigDecimal("26");

    private Money() {
    }

    /** Round a monetary amount to 2 decimals. */
    public static BigDecimal round(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE) : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Attendance ratio = daysWorked / 26 at high precision (not yet rounded to money). */
    public static BigDecimal attendanceRatio(BigDecimal daysWorked) {
        return daysWorked.divide(WORKING_DAYS, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
