package com.maram.payroll.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Salary band (CAT1, CAT3, ...). Reference data; full management arrives in Sprint 5.
 */
@Entity
@Table(name = "salary_categories")
@Getter
@Setter
@NoArgsConstructor
public class SalaryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    @Column(name = "base_multiplier")
    private BigDecimal baseMultiplier;
}
