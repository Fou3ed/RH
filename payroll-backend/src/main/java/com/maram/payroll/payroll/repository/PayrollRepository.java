package com.maram.payroll.payroll.repository;

import com.maram.payroll.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByPayrollPeriodIdOrderByEmployee_FullNameAsc(Long payrollPeriodId);

    Optional<Payroll> findByEmployeeIdAndPayrollPeriodId(Long employeeId, Long payrollPeriodId);
}
