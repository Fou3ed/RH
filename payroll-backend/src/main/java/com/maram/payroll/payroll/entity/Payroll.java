package com.maram.payroll.payroll.entity;

import com.maram.payroll.config.period.PayrollPeriod;
import com.maram.payroll.employee.entity.Employee;
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
 * The calculated monthly payroll for one employee in one period. Holds the full
 * component breakdown so a payslip can be reproduced exactly. Unique per
 * (employee, period).
 */
@Entity
@Table(name = "payroll")
@Getter
@Setter
@NoArgsConstructor
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_period_id", nullable = false)
    private PayrollPeriod payrollPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "days_worked", nullable = false)
    private BigDecimal daysWorked;

    @Column(name = "attendance_rate")
    private BigDecimal attendanceRate;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    @Column(name = "adjusted_salary")
    private BigDecimal adjustedSalary;

    @Column(name = "presence_allowance")
    private BigDecimal presenceAllowance;

    @Column(name = "transport_allowance")
    private BigDecimal transportAllowance;

    @Column(name = "diligence_allowance")
    private BigDecimal diligenceAllowance;

    @Column(name = "meal_allowance")
    private BigDecimal mealAllowance;

    @Column(name = "child_allowance")
    private BigDecimal childAllowance;

    @Column(name = "performance_bonus")
    private BigDecimal performanceBonus;

    @Column(name = "other_allowances")
    private BigDecimal otherAllowances;

    @Column(name = "total_allowances")
    private BigDecimal totalAllowances;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(name = "absence_penalty")
    private BigDecimal absencePenalty;

    @Column(name = "income_tax_irpp")
    private BigDecimal incomeTaxIrpp;

    @Column(name = "cnss_contribution")
    private BigDecimal cnssContribution;

    @Column(name = "health_insurance")
    private BigDecimal healthInsurance;

    @Column(name = "loan_repayment")
    private BigDecimal loanRepayment;

    @Column(name = "other_deductions")
    private BigDecimal otherDeductions;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false)
    private BigDecimal netSalary;

    @Column(name = "payment_status")
    private String paymentStatus = "DRAFT";

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference")
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
