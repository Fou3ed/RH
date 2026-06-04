package com.maram.payroll.payroll.repository;

import com.maram.payroll.payroll.entity.PerformanceRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerformanceRatingRepository extends JpaRepository<PerformanceRating, Long> {

    Optional<PerformanceRating> findByEmployeeIdAndPayrollPeriodId(Long employeeId, Long payrollPeriodId);
}
