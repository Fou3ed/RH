package com.maram.payroll.config.tax;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxConfigurationRequest(
        @NotNull Integer taxYear,
        @NotBlank String taxType,
        BigDecimal minTaxableIncome,
        BigDecimal maxTaxableIncome,
        @PositiveOrZero BigDecimal taxRate,
        BigDecimal taxCreditAmount,
        String familyStatusCode,
        Integer numberOfChildren,
        LocalDate effectiveDate,
        LocalDate endDate) {
}
