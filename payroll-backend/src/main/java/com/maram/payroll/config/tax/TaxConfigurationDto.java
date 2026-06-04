package com.maram.payroll.config.tax;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxConfigurationDto(
        Long id,
        Integer taxYear,
        String taxType,
        BigDecimal minTaxableIncome,
        BigDecimal maxTaxableIncome,
        BigDecimal taxRate,
        BigDecimal taxCreditAmount,
        String familyStatusCode,
        Integer numberOfChildren,
        LocalDate effectiveDate,
        LocalDate endDate) {

    public static TaxConfigurationDto from(TaxConfiguration t) {
        return new TaxConfigurationDto(
                t.getId(), t.getTaxYear(), t.getTaxType(),
                t.getMinTaxableIncome(), t.getMaxTaxableIncome(), t.getTaxRate(),
                t.getTaxCreditAmount(), t.getFamilyStatusCode(), t.getNumberOfChildren(),
                t.getEffectiveDate(), t.getEndDate());
    }
}
