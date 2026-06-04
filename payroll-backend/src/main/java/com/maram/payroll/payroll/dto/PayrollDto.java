package com.maram.payroll.payroll.dto;

import com.maram.payroll.payroll.entity.Payroll;

import java.math.BigDecimal;

public record PayrollDto(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal daysWorked,
        BigDecimal baseSalary,
        BigDecimal adjustedSalary,
        BigDecimal presenceAllowance,
        BigDecimal transportAllowance,
        BigDecimal diligenceAllowance,
        BigDecimal mealAllowance,
        BigDecimal childAllowance,
        BigDecimal performanceBonus,
        BigDecimal totalAllowances,
        BigDecimal grossSalary,
        BigDecimal incomeTaxIrpp,
        BigDecimal cnssContribution,
        BigDecimal healthInsurance,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        String paymentStatus) {

    public static PayrollDto from(Payroll p) {
        return new PayrollDto(
                p.getId(),
                p.getEmployee().getId(),
                p.getEmployee().getEmployeeId(),
                p.getEmployee().getFullName(),
                p.getDaysWorked(),
                p.getBaseSalary(),
                p.getAdjustedSalary(),
                p.getPresenceAllowance(),
                p.getTransportAllowance(),
                p.getDiligenceAllowance(),
                p.getMealAllowance(),
                p.getChildAllowance(),
                p.getPerformanceBonus(),
                p.getTotalAllowances(),
                p.getGrossSalary(),
                p.getIncomeTaxIrpp(),
                p.getCnssContribution(),
                p.getHealthInsurance(),
                p.getTotalDeductions(),
                p.getNetSalary(),
                p.getPaymentStatus());
    }
}
