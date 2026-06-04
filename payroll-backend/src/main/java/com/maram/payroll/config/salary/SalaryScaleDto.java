package com.maram.payroll.config.salary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryScaleDto(
        Long id,
        Long categoryId,
        String categoryCode,
        Integer echelon,
        Integer year,
        BigDecimal salaryMultiplier,
        BigDecimal annualSalary,
        BigDecimal increaseAmount,
        BigDecimal increasePercent,
        LocalDate validFrom,
        LocalDate validTo) {

    public static SalaryScaleDto from(SalaryScale s) {
        return new SalaryScaleDto(
                s.getId(),
                s.getCategory().getId(),
                s.getCategory().getCode(),
                s.getEchelon(),
                s.getYear(),
                s.getSalaryMultiplier(),
                s.getAnnualSalary(),
                s.getIncreaseAmount(),
                s.getIncreasePercent(),
                s.getValidFrom(),
                s.getValidTo());
    }
}
