package com.maram.payroll.reporting.service;

import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.config.period.PayrollPeriod;
import com.maram.payroll.config.period.PayrollPeriodRepository;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generates tax-declaration CSV files (IRPP and CNSS) for a payroll period, in a
 * simple per-employee layout suitable for government filing prep.
 */
@Service
public class TaxDeclarationService {

    private final PayrollRepository payrollRepository;
    private final PayrollPeriodRepository periodRepository;

    public TaxDeclarationService(PayrollRepository payrollRepository, PayrollPeriodRepository periodRepository) {
        this.payrollRepository = payrollRepository;
        this.periodRepository = periodRepository;
    }

    public record Declaration(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public Declaration irpp(Long periodId) {
        return build(periodId, "IRPP", "employee_id,full_name,cnss_number,gross,taxable,irpp",
                p -> String.join(",",
                        csv(p.getEmployee().getEmployeeId()),
                        csv(p.getEmployee().getFullName()),
                        csv(p.getEmployee().getCnssNumber()),
                        money(p.getGrossSalary()),
                        money(nz(p.getGrossSalary()).subtract(nz(p.getCnssContribution()))),
                        money(p.getIncomeTaxIrpp())));
    }

    @Transactional(readOnly = true)
    public Declaration cnss(Long periodId) {
        return build(periodId, "CNSS", "employee_id,full_name,cnss_number,gross,cnss",
                p -> String.join(",",
                        csv(p.getEmployee().getEmployeeId()),
                        csv(p.getEmployee().getFullName()),
                        csv(p.getEmployee().getCnssNumber()),
                        money(p.getGrossSalary()),
                        money(p.getCnssContribution())));
    }

    private Declaration build(Long periodId, String type, String header, java.util.function.Function<Payroll, String> rowFn) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", periodId));
        List<Payroll> rows = payrollRepository.findByPayrollPeriodIdOrderByEmployee_FullNameAsc(periodId);

        StringBuilder sb = new StringBuilder(header).append("\n");
        for (Payroll p : rows) {
            sb.append(rowFn.apply(p)).append("\n");
        }
        String fileName = "%s_declaration_%s.csv".formatted(type, period.getPeriodCode());
        return new Declaration(fileName, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String money(BigDecimal v) {
        return nz(v).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
