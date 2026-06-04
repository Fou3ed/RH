package com.maram.payroll.config.tax;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A tax rule: an IRPP bracket (min/max income + rate), or a flat-rate entry such
 * as CNSS (5.95%) or HEALTH. Versioned by {@code taxYear}.
 */
@Entity
@Table(name = "tax_configuration")
@Getter
@Setter
@NoArgsConstructor
public class TaxConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "tax_type", nullable = false)
    private String taxType; // IRPP, CNSS, HEALTH

    @Column(name = "min_taxable_income")
    private BigDecimal minTaxableIncome;

    @Column(name = "max_taxable_income")
    private BigDecimal maxTaxableIncome;

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Column(name = "tax_credit_amount")
    private BigDecimal taxCreditAmount;

    @Column(name = "family_status_code")
    private String familyStatusCode;

    @Column(name = "number_of_children")
    private Integer numberOfChildren;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
