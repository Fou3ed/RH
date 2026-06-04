package com.maram.payroll.attendance.dto;

import java.math.BigDecimal;

/**
 * One employee's aggregated attendance for a reporting period.
 */
public record AttendanceReportRow(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        int presentDays,
        int halfDays,
        int absentDays,
        int leaveDays,
        BigDecimal daysWorked,
        BigDecimal attendanceRate) {
}
