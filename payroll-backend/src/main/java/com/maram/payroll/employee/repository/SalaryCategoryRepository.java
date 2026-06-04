package com.maram.payroll.employee.repository;

import com.maram.payroll.employee.entity.SalaryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaryCategoryRepository extends JpaRepository<SalaryCategory, Long> {

    Optional<SalaryCategory> findByCode(String code);
}
