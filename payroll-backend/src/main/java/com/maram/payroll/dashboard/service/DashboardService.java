package com.maram.payroll.dashboard.service;

import com.maram.payroll.attendance.dto.AttendanceReportRow;
import com.maram.payroll.attendance.service.AttendanceService;
import com.maram.payroll.config.period.PayrollPeriod;
import com.maram.payroll.config.period.PayrollPeriodRepository;
import com.maram.payroll.dashboard.dto.HrDashboard;
import com.maram.payroll.dashboard.dto.PayrollDashboard;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.repository.EmployeeRepository;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceService attendanceService;
    private final PayrollRepository payrollRepository;
    private final PayrollPeriodRepository periodRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            AttendanceService attendanceService,
                            PayrollRepository payrollRepository,
                            PayrollPeriodRepository periodRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceService = attendanceService;
        this.payrollRepository = payrollRepository;
        this.periodRepository = periodRepository;
    }

    @Transactional(readOnly = true)
    public HrDashboard hrDashboard() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByEmploymentStatus("ACTIVE");

        YearMonth now = currentMonth();
        long newHires = employeeRepository.countByHireDateBetween(now.atDay(1), now.atEndOfMonth());

        List<HrDashboard.DepartmentCount> byDept = employeeRepository.countByDepartment().stream()
                .map(r -> new HrDashboard.DepartmentCount(
                        r[0] != null ? r[0].toString() : "—", ((Number) r[1]).longValue()))
                .toList();

        List<HrDashboard.RecentHire> recent = employeeRepository.findTop5ByOrderByHireDateDesc().stream()
                .map(this::toRecentHire).toList();

        BigDecimal attendanceRate = averageAttendanceRate(now);

        return new HrDashboard(total, active, total - active, newHires, attendanceRate, byDept, recent);
    }

    @Transactional(readOnly = true)
    public PayrollDashboard payrollDashboard() {
        long active = employeeRepository.countByEmploymentStatus("ACTIVE");
        PayrollPeriod latest = periodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc().stream()
                .findFirst().orElse(null);

        if (latest == null) {
            return new PayrollDashboard(null, null, null, active, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Payroll> rows = payrollRepository.findByPayrollPeriodIdOrderByEmployee_FullNameAsc(latest.getId());
        long calculated = rows.size();
        long approved = rows.stream().filter(p -> "APPROVED".equals(p.getPaymentStatus())).count();
        long pending = rows.stream().filter(p -> "DRAFT".equals(p.getPaymentStatus())).count();
        BigDecimal totalGross = rows.stream().map(Payroll::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = rows.stream().map(Payroll::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = active > 0
                ? BigDecimal.valueOf(calculated).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(active), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new PayrollDashboard(latest.getId(), latest.getPeriodCode(), latest.getStatus().name(),
                active, calculated, approved, pending, percent,
                totalGross.setScale(2, RoundingMode.HALF_UP), totalNet.setScale(2, RoundingMode.HALF_UP));
    }

    // ---- helpers ----

    private HrDashboard.RecentHire toRecentHire(Employee e) {
        return new HrDashboard.RecentHire(
                e.getEmployeeId(), e.getFullName(),
                e.getDepartment() != null ? e.getDepartment().getName() : null, e.getHireDate());
    }

    private BigDecimal averageAttendanceRate(YearMonth month) {
        List<AttendanceReportRow> rows = attendanceService.report(month.getYear(), month.getMonthValue(), null);
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = rows.stream().map(AttendanceReportRow::attendanceRate).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(rows.size()), 4, RoundingMode.HALF_UP);
    }

    /** Current month; extracted so it is the single source of "now" (Clock-injectable later). */
    private YearMonth currentMonth() {
        LocalDate today = LocalDate.now();
        return YearMonth.of(today.getYear(), today.getMonth());
    }
}
