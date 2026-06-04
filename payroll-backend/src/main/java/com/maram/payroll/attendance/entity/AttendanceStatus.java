package com.maram.payroll.attendance.entity;

import java.math.BigDecimal;

/**
 * Daily attendance status, with the day-fraction it contributes and a default
 * spreadsheet code (used by the Excel import / export).
 */
public enum AttendanceStatus {

    PRESENT(new BigDecimal("1.0"), "8", true),
    HALF_DAY(new BigDecimal("0.5"), "4", true),
    ABSENT(BigDecimal.ZERO, "A", false),
    LEAVE(new BigDecimal("1.0"), "C", false),
    HOLIDAY(new BigDecimal("1.0"), "H", false),
    WEEKEND(BigDecimal.ZERO, "W", false);

    private final BigDecimal dayFraction;
    private final String defaultCode;
    private final boolean countsAsWorked;

    AttendanceStatus(BigDecimal dayFraction, String defaultCode, boolean countsAsWorked) {
        this.dayFraction = dayFraction;
        this.defaultCode = defaultCode;
        this.countsAsWorked = countsAsWorked;
    }

    public BigDecimal dayFraction() {
        return dayFraction;
    }

    public String defaultCode() {
        return defaultCode;
    }

    public boolean countsAsWorked() {
        return countsAsWorked;
    }
}
