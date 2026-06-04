package com.maram.payroll.config.tax;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaxConfigurationService {

    private static final BigDecimal MAX = new BigDecimal("99999999");

    private final TaxConfigurationRepository repository;
    private final AuditService auditService;

    public TaxConfigurationService(TaxConfigurationRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TaxConfigurationDto> listByYear(int year) {
        return repository.findByTaxYearOrderByTaxTypeAscMinTaxableIncomeAsc(year).stream()
                .map(TaxConfigurationDto::from).toList();
    }

    @Transactional
    public TaxConfigurationDto create(TaxConfigurationRequest request) {
        validateBracket(request, null);
        TaxConfiguration tax = new TaxConfiguration();
        apply(tax, request);
        TaxConfiguration saved = repository.save(tax);
        auditService.log("TAX_CONFIG", saved.getId(), "CREATE",
                "Created %s %d bracket".formatted(request.taxType(), request.taxYear()));
        return TaxConfigurationDto.from(saved);
    }

    @Transactional
    public TaxConfigurationDto update(Long id, TaxConfigurationRequest request) {
        TaxConfiguration tax = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax configuration", id));
        validateBracket(request, id);
        apply(tax, request);
        auditService.log("TAX_CONFIG", id, "UPDATE", "Updated %s bracket".formatted(request.taxType()));
        return TaxConfigurationDto.from(tax);
    }

    @Transactional
    public void delete(Long id) {
        TaxConfiguration tax = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax configuration", id));
        repository.delete(tax);
        auditService.log("TAX_CONFIG", id, "DELETE", "Deleted tax configuration " + id);
    }

    /** IRPP brackets within the same year must not overlap. */
    private void validateBracket(TaxConfigurationRequest request, Long excludeId) {
        if (!"IRPP".equalsIgnoreCase(request.taxType())) {
            return;
        }
        BigDecimal newMin = request.minTaxableIncome() != null ? request.minTaxableIncome() : BigDecimal.ZERO;
        BigDecimal newMax = request.maxTaxableIncome() != null ? request.maxTaxableIncome() : MAX;
        if (newMin.compareTo(newMax) >= 0) {
            throw new IllegalArgumentException("Bracket min must be less than max");
        }

        for (TaxConfiguration existing : repository.findByTaxYearAndTaxType(request.taxYear(), "IRPP")) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            BigDecimal exMin = existing.getMinTaxableIncome() != null ? existing.getMinTaxableIncome() : BigDecimal.ZERO;
            BigDecimal exMax = existing.getMaxTaxableIncome() != null ? existing.getMaxTaxableIncome() : MAX;
            // Half-open ranges [min,max) overlap iff newMin < exMax AND exMin < newMax.
            if (newMin.compareTo(exMax) < 0 && exMin.compareTo(newMax) < 0) {
                throw new IllegalArgumentException(
                        "IRPP bracket overlaps an existing one [%s, %s)".formatted(exMin, exMax));
            }
        }
    }

    private void apply(TaxConfiguration tax, TaxConfigurationRequest request) {
        tax.setTaxYear(request.taxYear());
        tax.setTaxType(request.taxType().toUpperCase());
        tax.setMinTaxableIncome(request.minTaxableIncome());
        tax.setMaxTaxableIncome(request.maxTaxableIncome());
        tax.setTaxRate(request.taxRate());
        tax.setTaxCreditAmount(request.taxCreditAmount());
        tax.setFamilyStatusCode(request.familyStatusCode());
        tax.setNumberOfChildren(request.numberOfChildren());
        tax.setEffectiveDate(request.effectiveDate() != null
                ? request.effectiveDate() : LocalDate.of(request.taxYear(), 1, 1));
        tax.setEndDate(request.endDate());
    }
}
