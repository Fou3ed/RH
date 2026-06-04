package com.maram.payroll.payroll.service;

import com.maram.payroll.attendance.entity.Attendance;
import com.maram.payroll.attendance.repository.AttendanceRepository;
import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.config.allowance.AllowanceConfig;
import com.maram.payroll.config.allowance.AllowanceConfigRepository;
import com.maram.payroll.config.period.PayrollPeriod;
import com.maram.payroll.config.period.PayrollPeriodRepository;
import com.maram.payroll.config.period.PeriodStatus;
import com.maram.payroll.config.salary.SalaryScale;
import com.maram.payroll.config.salary.SalaryScaleRepository;
import com.maram.payroll.config.tax.TaxConfiguration;
import com.maram.payroll.config.tax.TaxConfigurationRepository;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.repository.EmployeeRepository;
import com.maram.payroll.payroll.calculator.AllowanceCalculator;
import com.maram.payroll.payroll.calculator.CNSSCalculator;
import com.maram.payroll.payroll.calculator.IRPPTaxCalculator;
import com.maram.payroll.payroll.calculator.Money;
import com.maram.payroll.payroll.calculator.PerformanceBonusCalculator;
import com.maram.payroll.payroll.calculator.SalaryCalculator;
import com.maram.payroll.payroll.dto.PayrollRunSummary;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import com.maram.payroll.payroll.repository.PerformanceRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The payroll engine. For a period it computes, per active employee:
 *
 * <pre>
 * base        = configuredBase × salaryScaleMultiplier(category, échelon, year)
 * adjusted    = base × (daysWorked / 26)                  ← prorates for absences
 * allowances  = presence + transport + diligence + meal + child + performance
 * gross       = adjusted + allowances
 * cnss        = gross × cnssRate
 * irpp        = progressive( gross − cnss )               ← CNSS deductible before IRPP
 * deductions  = irpp + cnss + health
 * net         = gross − deductions
 * </pre>
 *
 * Employees missing a base salary or a salary scale are skipped (reported in the
 * run summary) rather than failing the whole batch.
 */
@Service
public class PayrollCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PayrollCalculationService.class);
    private static final BigDecimal DEFAULT_CNSS_RATE = new BigDecimal("5.95");

    private final EmployeeRepository employeeRepository;
    private final PayrollPeriodRepository periodRepository;
    private final PayrollRepository payrollRepository;
    private final SalaryScaleRepository salaryScaleRepository;
    private final AllowanceConfigRepository allowanceConfigRepository;
    private final TaxConfigurationRepository taxConfigurationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PerformanceRatingRepository performanceRatingRepository;

    private final SalaryCalculator salaryCalculator;
    private final AllowanceCalculator allowanceCalculator;
    private final PerformanceBonusCalculator performanceBonusCalculator;
    private final IRPPTaxCalculator irppTaxCalculator;
    private final CNSSCalculator cnssCalculator;
    private final AuditService auditService;

    public PayrollCalculationService(EmployeeRepository employeeRepository,
                                     PayrollPeriodRepository periodRepository,
                                     PayrollRepository payrollRepository,
                                     SalaryScaleRepository salaryScaleRepository,
                                     AllowanceConfigRepository allowanceConfigRepository,
                                     TaxConfigurationRepository taxConfigurationRepository,
                                     AttendanceRepository attendanceRepository,
                                     PerformanceRatingRepository performanceRatingRepository,
                                     SalaryCalculator salaryCalculator,
                                     AllowanceCalculator allowanceCalculator,
                                     PerformanceBonusCalculator performanceBonusCalculator,
                                     IRPPTaxCalculator irppTaxCalculator,
                                     CNSSCalculator cnssCalculator,
                                     AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.periodRepository = periodRepository;
        this.payrollRepository = payrollRepository;
        this.salaryScaleRepository = salaryScaleRepository;
        this.allowanceConfigRepository = allowanceConfigRepository;
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.attendanceRepository = attendanceRepository;
        this.performanceRatingRepository = performanceRatingRepository;
        this.salaryCalculator = salaryCalculator;
        this.allowanceCalculator = allowanceCalculator;
        this.performanceBonusCalculator = performanceBonusCalculator;
        this.irppTaxCalculator = irppTaxCalculator;
        this.cnssCalculator = cnssCalculator;
        this.auditService = auditService;
    }

    @Transactional
    public PayrollRunSummary calculateForPeriod(Long periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", periodId));
        if (period.getStatus() == PeriodStatus.PAID) {
            throw new IllegalArgumentException("Cannot recalculate a PAID period");
        }

        int year = period.getPeriodYear();
        List<TaxConfiguration> taxRows = taxConfigurationRepository.findByTaxYearAndTaxType(year, "IRPP");
        List<IRPPTaxCalculator.Bracket> brackets = taxRows.stream()
                .sorted(Comparator.comparing(t -> t.getMinTaxableIncome() != null
                        ? t.getMinTaxableIncome() : BigDecimal.ZERO))
                .map(t -> new IRPPTaxCalculator.Bracket(t.getMinTaxableIncome(), t.getMaxTaxableIncome(), t.getTaxRate()))
                .toList();
        BigDecimal cnssRate = taxConfigurationRepository.findByTaxYearAndTaxType(year, "CNSS").stream()
                .findFirst().map(TaxConfiguration::getTaxRate).orElse(DEFAULT_CNSS_RATE);
        BigDecimal healthRate = taxConfigurationRepository.findByTaxYearAndTaxType(year, "HEALTH").stream()
                .findFirst().map(TaxConfiguration::getTaxRate).orElse(BigDecimal.ZERO);
        List<AllowanceConfig> allowanceConfigs = effectiveAllowances(period);

        List<Employee> employees = employeeRepository.findByEmploymentStatus("ACTIVE");
        List<String> skipped = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO, totalNet = BigDecimal.ZERO, totalDeductions = BigDecimal.ZERO;
        int calculated = 0;

        for (Employee employee : employees) {
            try {
                Payroll payroll = calculateForEmployee(employee, period, allowanceConfigs, brackets, cnssRate, healthRate);
                if (payroll == null) {
                    skipped.add(employee.getEmployeeId() + " (missing base salary or salary scale)");
                    continue;
                }
                payrollRepository.save(payroll);
                calculated++;
                totalGross = totalGross.add(payroll.getGrossSalary());
                totalNet = totalNet.add(payroll.getNetSalary());
                totalDeductions = totalDeductions.add(payroll.getTotalDeductions());
            } catch (Exception e) {
                log.error("Payroll calculation failed for employee {}", employee.getEmployeeId(), e);
                skipped.add(employee.getEmployeeId() + " (error: " + e.getMessage() + ")");
            }
        }

        if (period.getStatus() == PeriodStatus.DRAFT) {
            period.setStatus(PeriodStatus.PROCESSING);
        }
        auditService.log("PAYROLL", periodId, "CALCULATE",
                "Calculated %d payrolls for %s (%d skipped)".formatted(calculated, period.getPeriodCode(), skipped.size()));

        return new PayrollRunSummary(periodId, period.getPeriodCode(), calculated, skipped.size(),
                Money.round(totalGross), Money.round(totalDeductions), Money.round(totalNet), skipped);
    }

    private Payroll calculateForEmployee(Employee employee, PayrollPeriod period,
                                         List<AllowanceConfig> allowanceConfigs,
                                         List<IRPPTaxCalculator.Bracket> brackets,
                                         BigDecimal cnssRate, BigDecimal healthRate) {
        BigDecimal configuredBase = employee.getBaseSalary();
        if (configuredBase == null) {
            return null;
        }
        SalaryScale scale = salaryScaleRepository
                .findByCategoryIdAndEchelonAndYear(employee.getCategory().getId(),
                        employee.getEchelon(), period.getPeriodYear())
                .orElse(null);
        if (scale == null) {
            return null;
        }

        BigDecimal baseSalary = salaryCalculator.calculateBaseSalary(configuredBase, scale.getSalaryMultiplier());
        BigDecimal daysWorked = daysWorked(employee, period);
        BigDecimal adjustedSalary = salaryCalculator.adjustForAttendance(baseSalary, daysWorked);

        // Allowances
        BigDecimal presence = BigDecimal.ZERO, transport = BigDecimal.ZERO,
                diligence = BigDecimal.ZERO, meal = BigDecimal.ZERO, child = BigDecimal.ZERO;
        for (AllowanceConfig cfg : allowanceConfigs) {
            switch (cfg.getAllowanceType()) {
                case "PRESENCE" -> presence = allowanceCalculator.component(cfg.getAmount(), cfg.isAttendanceAdjusted(), daysWorked);
                case "TRANSPORT" -> transport = allowanceCalculator.component(cfg.getAmount(), cfg.isAttendanceAdjusted(), daysWorked);
                case "DILIGENCE" -> diligence = allowanceCalculator.component(cfg.getAmount(), cfg.isAttendanceAdjusted(), daysWorked);
                case "MEAL" -> meal = allowanceCalculator.component(cfg.getAmount(), cfg.isAttendanceAdjusted(), daysWorked);
                case "CHILD" -> child = allowanceCalculator.child(cfg.getAmount(), employee.getNumberOfChildren());
                default -> { /* unknown type ignored */ }
            }
        }
        Integer rating = performanceRatingRepository
                .findByEmployeeIdAndPayrollPeriodId(employee.getId(), period.getId())
                .map(r -> r.getRatingScore()).orElse(null);
        BigDecimal performance = performanceBonusCalculator.calculate(baseSalary, rating);

        BigDecimal totalAllowances = Money.round(
                presence.add(transport).add(diligence).add(meal).add(child).add(performance));
        BigDecimal gross = Money.round(adjustedSalary.add(totalAllowances));

        // Deductions
        BigDecimal cnss = cnssCalculator.calculate(gross, cnssRate);
        BigDecimal taxableBase = gross.subtract(cnss).max(BigDecimal.ZERO);
        BigDecimal irpp = irppTaxCalculator.calculate(taxableBase, brackets, BigDecimal.ZERO);
        BigDecimal health = cnssCalculator.calculate(gross, healthRate); // same "rate × gross" shape
        BigDecimal totalDeductions = Money.round(irpp.add(cnss).add(health));

        BigDecimal net = Money.round(gross.subtract(totalDeductions));

        Payroll payroll = payrollRepository
                .findByEmployeeIdAndPayrollPeriodId(employee.getId(), period.getId())
                .orElseGet(Payroll::new);
        payroll.setEmployee(employee);
        payroll.setPayrollPeriod(period);
        payroll.setDaysWorked(daysWorked);
        payroll.setAttendanceRate(daysWorked.divide(Money.WORKING_DAYS, 4, RoundingMode.HALF_UP));
        payroll.setBaseSalary(baseSalary);
        payroll.setAdjustedSalary(adjustedSalary);
        payroll.setPresenceAllowance(presence);
        payroll.setTransportAllowance(transport);
        payroll.setDiligenceAllowance(diligence);
        payroll.setMealAllowance(meal);
        payroll.setChildAllowance(child);
        payroll.setPerformanceBonus(performance);
        payroll.setOtherAllowances(BigDecimal.ZERO);
        payroll.setTotalAllowances(totalAllowances);
        payroll.setGrossSalary(gross);
        payroll.setAbsencePenalty(BigDecimal.ZERO); // proration already accounts for absences
        payroll.setIncomeTaxIrpp(irpp);
        payroll.setCnssContribution(cnss);
        payroll.setHealthInsurance(health);
        payroll.setLoanRepayment(BigDecimal.ZERO);
        payroll.setOtherDeductions(BigDecimal.ZERO);
        payroll.setTotalDeductions(totalDeductions);
        payroll.setNetSalary(net);
        if (payroll.getPaymentStatus() == null) {
            payroll.setPaymentStatus("DRAFT");
        }
        if (payroll.getCreatedBy() == null) {
            payroll.setCreatedBy(SecurityUtils.getCurrentUsername().orElse("system"));
        }
        return payroll;
    }

    /** Days worked for the period: present(1) + half-day(0.5) + paid leave/holiday(1), capped at working days. */
    private BigDecimal daysWorked(Employee employee, PayrollPeriod period) {
        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDate(
                        employee.getId(), period.getStartDate(), period.getEndDate());
        if (records.isEmpty()) {
            // No attendance captured → assume the employee worked the full period.
            return new BigDecimal(period.getWorkingDays() != null ? period.getWorkingDays() : 26);
        }
        BigDecimal worked = BigDecimal.ZERO;
        for (Attendance a : records) {
            worked = worked.add(switch (a.getAttendanceStatus()) {
                case PRESENT, LEAVE, HOLIDAY -> BigDecimal.ONE;
                case HALF_DAY -> new BigDecimal("0.5");
                case ABSENT, WEEKEND -> BigDecimal.ZERO;
            });
        }
        BigDecimal cap = new BigDecimal(period.getWorkingDays() != null ? period.getWorkingDays() : 26);
        return worked.min(cap);
    }

    private List<AllowanceConfig> effectiveAllowances(PayrollPeriod period) {
        LocalDate periodEnd = period.getEndDate();
        return allowanceConfigRepository.findByOrderByAllowanceTypeAsc().stream()
                .filter(c -> !c.getEffectiveDate().isAfter(periodEnd))
                .filter(c -> c.getEndDate() == null || !c.getEndDate().isBefore(period.getStartDate()))
                .toList();
    }
}
