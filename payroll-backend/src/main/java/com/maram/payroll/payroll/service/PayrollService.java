package com.maram.payroll.payroll.service;

import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.payroll.dto.PayrollDto;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read + approval operations on calculated payroll rows.
 */
@Service
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final AuditService auditService;

    public PayrollService(PayrollRepository payrollRepository, AuditService auditService) {
        this.payrollRepository = payrollRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PayrollDto> listByPeriod(Long periodId) {
        return payrollRepository.findByPayrollPeriodIdOrderByEmployee_FullNameAsc(periodId).stream()
                .map(PayrollDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PayrollDto get(Long id) {
        return PayrollDto.from(find(id));
    }

    @Transactional
    public PayrollDto approve(Long id) {
        Payroll payroll = find(id);
        if (!"DRAFT".equals(payroll.getPaymentStatus())) {
            throw new IllegalArgumentException("Only DRAFT payroll can be approved (current: "
                    + payroll.getPaymentStatus() + ")");
        }
        payroll.setPaymentStatus("APPROVED");
        payroll.setApprovedBy(SecurityUtils.getCurrentUsername().orElse("system"));
        payroll.setApprovedAt(LocalDateTime.now());
        auditService.log("PAYROLL", id, "APPROVE", "Approved payroll " + id);
        return PayrollDto.from(payroll);
    }

    private Payroll find(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", id));
    }
}
