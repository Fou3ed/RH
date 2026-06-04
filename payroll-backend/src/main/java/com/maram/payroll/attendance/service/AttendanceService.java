package com.maram.payroll.attendance.service;

import com.maram.payroll.attendance.dto.AttendanceDto;
import com.maram.payroll.attendance.dto.AttendanceReportRow;
import com.maram.payroll.attendance.dto.AttendanceRequest;
import com.maram.payroll.attendance.dto.AttendanceSummaryDto;
import com.maram.payroll.attendance.dto.AttendanceUpdateRequest;
import com.maram.payroll.attendance.entity.Attendance;
import com.maram.payroll.attendance.entity.AttendanceStatus;
import com.maram.payroll.attendance.repository.AttendanceRepository;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    /** Standard working days per month used for the attendance rate. */
    private static final BigDecimal WORKING_DAYS = new BigDecimal("26");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AttendanceDto record(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));

        validateNotFuture(request.attendanceDate());
        AttendanceStatus status = parseStatus(request.status());

        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), request.attendanceDate())) {
            throw new ConflictException("Attendance already recorded for %s on %s"
                    .formatted(employee.getEmployeeId(), request.attendanceDate()));
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(request.attendanceDate());
        attendance.setDayOfWeek(request.attendanceDate().getDayOfWeek().name());
        applyStatus(attendance, status, request.hoursWorked());
        attendance.setNotes(request.notes());
        attendance.setAbsenceReason(request.absenceReason());

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log("ATTENDANCE", saved.getId(), "CREATE",
                "Recorded %s for employee %d on %s".formatted(status, employee.getId(), request.attendanceDate()));
        return AttendanceDto.from(saved);
    }

    @Transactional
    public AttendanceDto update(Long id, AttendanceUpdateRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", id));
        AttendanceStatus status = parseStatus(request.status());
        applyStatus(attendance, status, request.hoursWorked());
        attendance.setNotes(request.notes());
        attendance.setAbsenceReason(request.absenceReason());
        auditService.log("ATTENDANCE", id, "UPDATE", "Updated attendance to " + status);
        return AttendanceDto.from(attendance);
    }

    @Transactional
    public void delete(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", id));
        attendanceRepository.delete(attendance);
        auditService.log("ATTENDANCE", id, "DELETE", "Deleted attendance " + id);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryDto monthlySummary(Long employeeId, int year, int month) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        YearMonth ym = YearMonth.of(year, month);
        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDate(
                        employeeId, ym.atDay(1), ym.atEndOfMonth());

        int present = count(records, AttendanceStatus.PRESENT);
        int half = count(records, AttendanceStatus.HALF_DAY);
        int absent = count(records, AttendanceStatus.ABSENT);
        int leave = count(records, AttendanceStatus.LEAVE);

        BigDecimal daysWorked = BigDecimal.valueOf(present)
                .add(BigDecimal.valueOf(half).multiply(new BigDecimal("0.5")));
        BigDecimal rate = daysWorked.divide(WORKING_DAYS, 4, RoundingMode.HALF_UP);

        return new AttendanceSummaryDto(
                employee.getId(), employee.getFullName(), year, month,
                present, half, absent, leave, daysWorked, rate,
                records.stream().map(AttendanceDto::from).toList());
    }

    /**
     * Aggregated attendance per employee for a month, optionally restricted to a
     * department, sorted by attendance rate ascending (worst attendance first —
     * i.e. top absentees at the top).
     */
    @Transactional(readOnly = true)
    public List<AttendanceReportRow> report(int year, int month, Long departmentId) {
        YearMonth ym = YearMonth.of(year, month);
        List<Attendance> all = attendanceRepository.findInRange(ym.atDay(1), ym.atEndOfMonth(), departmentId);

        Map<Long, List<Attendance>> byEmployee = new LinkedHashMap<>();
        for (Attendance a : all) {
            byEmployee.computeIfAbsent(a.getEmployee().getId(), k -> new java.util.ArrayList<>()).add(a);
        }

        return byEmployee.values().stream().map(records -> {
            Employee emp = records.get(0).getEmployee();
            int present = count(records, AttendanceStatus.PRESENT);
            int half = count(records, AttendanceStatus.HALF_DAY);
            int absent = count(records, AttendanceStatus.ABSENT);
            int leave = count(records, AttendanceStatus.LEAVE);
            BigDecimal daysWorked = BigDecimal.valueOf(present)
                    .add(BigDecimal.valueOf(half).multiply(new BigDecimal("0.5")));
            BigDecimal rate = daysWorked.divide(WORKING_DAYS, 4, RoundingMode.HALF_UP);
            return new AttendanceReportRow(
                    emp.getId(), emp.getEmployeeId(), emp.getFullName(),
                    emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                    present, half, absent, leave, daysWorked, rate);
        }).sorted(Comparator.comparing(AttendanceReportRow::attendanceRate)).toList();
    }

    // ---- helpers ----

    private void applyStatus(Attendance attendance, AttendanceStatus status, BigDecimal hoursWorked) {
        attendance.setAttendanceStatus(status);
        attendance.setAttendanceCode(status.defaultCode());
        attendance.setDaysFraction(status.dayFraction());
        attendance.setHoursWorked(hoursWorked);
        attendance.setPaidLeave(status == AttendanceStatus.LEAVE);
    }

    private void validateNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot record attendance for a future date");
        }
    }

    private AttendanceStatus parseStatus(String status) {
        try {
            return AttendanceStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid attendance status: " + status);
        }
    }

    private static int count(List<Attendance> records, AttendanceStatus status) {
        return (int) records.stream().filter(a -> a.getAttendanceStatus() == status).count();
    }
}
