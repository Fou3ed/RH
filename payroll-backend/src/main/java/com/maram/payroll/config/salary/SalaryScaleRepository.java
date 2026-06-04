package com.maram.payroll.config.salary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryScaleRepository extends JpaRepository<SalaryScale, Long> {

    List<SalaryScale> findByYearOrderByCategoryAscEchelonAsc(int year);

    /** Lookup a specific cell; also used to validate year-over-year increases. */
    Optional<SalaryScale> findByCategoryIdAndEchelonAndYear(Long categoryId, Integer echelon, Integer year);
}
