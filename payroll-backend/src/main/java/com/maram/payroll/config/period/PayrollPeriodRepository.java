package com.maram.payroll.config.period;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    boolean existsByPeriodYearAndPeriodMonth(int year, int month);

    List<PayrollPeriod> findAllByOrderByPeriodYearDescPeriodMonthDesc();
}
