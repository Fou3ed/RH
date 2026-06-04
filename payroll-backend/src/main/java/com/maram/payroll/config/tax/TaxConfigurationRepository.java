package com.maram.payroll.config.tax;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {

    List<TaxConfiguration> findByTaxYearOrderByTaxTypeAscMinTaxableIncomeAsc(int taxYear);

    List<TaxConfiguration> findByTaxYearAndTaxType(int taxYear, String taxType);
}
