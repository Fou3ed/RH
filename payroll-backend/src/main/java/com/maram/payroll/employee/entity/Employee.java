package com.maram.payroll.employee.entity;

import com.maram.payroll.common.domain.Auditable;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Employee master record. The single source of truth for everything downstream
 * (attendance, payroll, reporting).
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SalaryCategory category;

    private Integer echelon;

    @Column(name = "base_salary")
    private BigDecimal baseSalary;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "family_status")
    private String familyStatus;

    @Column(name = "number_of_children")
    private Integer numberOfChildren;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "cnss_number")
    private String cnssNumber;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;
    private String address;
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "employment_status")
    private String employmentStatus = "ACTIVE";

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason")
    private String terminationReason;
}
