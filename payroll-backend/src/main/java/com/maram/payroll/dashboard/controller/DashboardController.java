package com.maram.payroll.dashboard.controller;

import com.maram.payroll.dashboard.dto.HrDashboard;
import com.maram.payroll.dashboard.dto.PayrollDashboard;
import com.maram.payroll.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Aggregated KPIs for HR and finance")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "HR KPIs: headcount, status mix, departments, attendance, recent hires")
    public HrDashboard hr() {
        return dashboardService.hrDashboard();
    }

    @GetMapping("/payroll")
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "Payroll KPIs for the most recent period")
    public PayrollDashboard payroll() {
        return dashboardService.payrollDashboard();
    }
}
