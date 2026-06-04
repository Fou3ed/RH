package com.maram.payroll.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * HR KPI snapshot.
 *
 * @param attendanceRateThisMonth average attendance rate (0–1) across employees with records this month
 */
public record HrDashboard(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long newHiresThisMonth,
        BigDecimal attendanceRateThisMonth,
        List<DepartmentCount> byDepartment,
        List<RecentHire> recentHires) {

    public record DepartmentCount(String department, long count) {
    }

    public record RecentHire(String employeeId, String fullName, String department, LocalDate hireDate) {
    }
}
