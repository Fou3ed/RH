package com.maram.payroll.attendance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-employee monthly attendance summary.
 *
 * @param daysWorked     present days + half of half-days
 * @param attendanceRate daysWorked / standard working days (26), 0–1
 */
public record AttendanceSummaryDto(
        Long employeeId,
        String employeeName,
        int year,
        int month,
        int presentDays,
        int halfDays,
        int absentDays,
        int leaveDays,
        BigDecimal daysWorked,
        BigDecimal attendanceRate,
        List<AttendanceDto> records) {
}
