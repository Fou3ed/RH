package com.maram.payroll.config.period;

import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class PayrollPeriodService {

    private final PayrollPeriodRepository repository;
    private final AuditService auditService;

    public PayrollPeriodService(PayrollPeriodRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PayrollPeriodDtos.Response> list() {
        return repository.findAllByOrderByPeriodYearDescPeriodMonthDesc().stream()
                .map(PayrollPeriodDtos.Response::from).toList();
    }

    @Transactional
    public PayrollPeriodDtos.Response create(PayrollPeriodDtos.CreateRequest request) {
        if (repository.existsByPeriodYearAndPeriodMonth(request.periodYear(), request.periodMonth())) {
            throw new ConflictException("A payroll period already exists for %d-%02d"
                    .formatted(request.periodYear(), request.periodMonth()));
        }

        YearMonth ym = YearMonth.of(request.periodYear(), request.periodMonth());
        PayrollPeriod period = new PayrollPeriod();
        period.setPeriodCode("%d-%02d".formatted(request.periodYear(), request.periodMonth()));
        period.setPeriodMonth(request.periodMonth());
        period.setPeriodYear(request.periodYear());
        period.setStartDate(request.startDate() != null ? request.startDate() : ym.atDay(1));
        period.setEndDate(request.endDate() != null ? request.endDate() : ym.atEndOfMonth());
        period.setWorkingDays(request.workingDays() != null ? request.workingDays() : 26);
        period.setHolidaysInPeriod(request.holidaysInPeriod() != null ? request.holidaysInPeriod() : 0);
        period.setStatus(PeriodStatus.DRAFT);

        PayrollPeriod saved = repository.save(period);
        auditService.log("PAYROLL_PERIOD", saved.getId(), "CREATE", "Created period " + saved.getPeriodCode());
        return PayrollPeriodDtos.Response.from(saved);
    }

    @Transactional
    public PayrollPeriodDtos.Response transition(Long id, String targetStatus) {
        PayrollPeriod period = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", id));

        PeriodStatus target;
        try {
            target = PeriodStatus.valueOf(targetStatus.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + targetStatus);
        }

        if (!period.getStatus().canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    "Cannot transition from %s to %s".formatted(period.getStatus(), target));
        }

        period.setStatus(target);
        applyTimestamps(period, target);
        auditService.log("PAYROLL_PERIOD", id, "TRANSITION",
                "Period %s → %s".formatted(period.getPeriodCode(), target));
        return PayrollPeriodDtos.Response.from(period);
    }

    @Transactional
    public void delete(Long id) {
        PayrollPeriod period = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", id));
        if (!PeriodStatus.mutableStatuses().contains(period.getStatus())) {
            throw new ConflictException("Only DRAFT periods can be deleted (current: " + period.getStatus() + ")");
        }
        repository.delete(period);
        auditService.log("PAYROLL_PERIOD", id, "DELETE", "Deleted period " + period.getPeriodCode());
    }

    private void applyTimestamps(PayrollPeriod period, PeriodStatus target) {
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case PROCESSING -> period.setProcessedAt(now);
            case FINALIZED -> {
                period.setApprovedAt(now);
                period.setApprovedBy(SecurityUtils.getCurrentUsername().orElse("system"));
            }
            case PAID -> period.setPaidAt(now);
            default -> { /* no timestamp for DRAFT/LOCKED */ }
        }
    }
}
