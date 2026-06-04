package com.maram.payroll.payroll.controller;

import com.maram.payroll.payroll.dto.PayrollDto;
import com.maram.payroll.payroll.dto.PayrollRunSummary;
import com.maram.payroll.payroll.service.PayrollCalculationService;
import com.maram.payroll.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payroll")
@Tag(name = "Payroll", description = "Payroll calculation, review & approval")
public class PayrollController {

    private final PayrollCalculationService calculationService;
    private final PayrollService payrollService;

    public PayrollController(PayrollCalculationService calculationService, PayrollService payrollService) {
        this.calculationService = calculationService;
        this.payrollService = payrollService;
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('payroll.calculate')")
    @Operation(summary = "Calculate payroll for all active employees in a period")
    public PayrollRunSummary calculate(@RequestParam("periodId") Long periodId) {
        return calculationService.calculateForPeriod(periodId);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "List calculated payroll for a period")
    public List<PayrollDto> list(@RequestParam("periodId") Long periodId) {
        return payrollService.listByPeriod(periodId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "Get a single payroll record")
    public PayrollDto get(@PathVariable Long id) {
        return payrollService.get(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('payroll.approve')")
    @Operation(summary = "Approve a draft payroll record")
    public PayrollDto approve(@PathVariable Long id) {
        return payrollService.approve(id);
    }
}
