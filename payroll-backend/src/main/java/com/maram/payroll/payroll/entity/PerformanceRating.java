package com.maram.payroll.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Monthly performance rating used as the basis for the performance bonus
 * (8 = excellent, 7 = good, 0 = absent). Read by the payroll engine.
 */
@Entity
@Table(name = "performance_ratings")
@Getter
@Setter
@NoArgsConstructor
public class PerformanceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    @Column(name = "rating_score")
    private Integer ratingScore;

    @Column(name = "rating_category")
    private String ratingCategory;
}
