package com.maram.payroll.config.allowance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllowanceConfigRepository extends JpaRepository<AllowanceConfig, Long> {

    List<AllowanceConfig> findByOrderByAllowanceTypeAsc();
}
