package com.maram.payroll.config.salary;

import com.maram.payroll.employee.entity.SalaryCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Salary multiplier for a (category, échelon, year). Versioned by year so historical
 * scales are preserved. {@code base × multiplier} yields the monthly base salary.
 */
@Entity
@Table(name = "salary_scales")
@Getter
@Setter
@NoArgsConstructor
public class SalaryScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SalaryCategory category;

    @Column(nullable = false)
    private Integer echelon;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "salary_multiplier", nullable = false)
    private BigDecimal salaryMultiplier;

    @Column(name = "annual_salary")
    private BigDecimal annualSalary;

    @Column(name = "increase_amount")
    private BigDecimal increaseAmount;

    @Column(name = "increase_percent")
    private BigDecimal increasePercent;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
