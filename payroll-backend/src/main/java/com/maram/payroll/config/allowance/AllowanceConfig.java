package com.maram.payroll.config.allowance;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A company-wide default allowance amount, effective-dated.
 */
@Entity
@Table(name = "allowance_config")
@Getter
@Setter
@NoArgsConstructor
public class AllowanceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allowance_type", nullable = false)
    private String allowanceType; // PRESENCE, TRANSPORT, DILIGENCE, MEAL, CHILD

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "attendance_adjusted", nullable = false)
    private boolean attendanceAdjusted;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
