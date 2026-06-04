package com.maram.payroll.attendance.dto;

import com.maram.payroll.attendance.entity.Attendance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceDto(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate attendanceDate,
        String attendanceStatus,
        String attendanceCode,
        BigDecimal daysFraction,
        BigDecimal hoursWorked,
        String notes,
        String absenceReason,
        boolean paidLeave) {

    public static AttendanceDto from(Attendance a) {
        return new AttendanceDto(
                a.getId(),
                a.getEmployee().getId(),
                a.getEmployee().getFullName(),
                a.getAttendanceDate(),
                a.getAttendanceStatus().name(),
                a.getAttendanceCode(),
                a.getDaysFraction(),
                a.getHoursWorked(),
                a.getNotes(),
                a.getAbsenceReason(),
                a.isPaidLeave());
    }
}
