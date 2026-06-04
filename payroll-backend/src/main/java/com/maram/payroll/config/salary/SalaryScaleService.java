package com.maram.payroll.config.salary;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.employee.entity.SalaryCategory;
import com.maram.payroll.employee.repository.SalaryCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalaryScaleService {

    private final SalaryScaleRepository repository;
    private final SalaryCategoryRepository categoryRepository;
    private final AuditService auditService;

    public SalaryScaleService(SalaryScaleRepository repository,
                              SalaryCategoryRepository categoryRepository,
                              AuditService auditService) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SalaryScaleDto> listByYear(int year) {
        return repository.findByYearOrderByCategoryAscEchelonAsc(year).stream()
                .map(SalaryScaleDto::from).toList();
    }

    @Transactional
    public SalaryScaleDto create(SalaryScaleRequest request) {
        SalaryCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Salary category", request.categoryId()));

        repository.findByCategoryIdAndEchelonAndYear(request.categoryId(), request.echelon(), request.year())
                .ifPresent(s -> {
                    throw new ConflictException("Salary scale already exists for that category/échelon/year");
                });

        validateYearOverYear(request.categoryId(), request.echelon(), request.year(), request.salaryMultiplier());

        SalaryScale scale = new SalaryScale();
        scale.setCategory(category);
        apply(scale, request);

        SalaryScale saved = repository.save(scale);
        auditService.log("SALARY_SCALE", saved.getId(), "CREATE",
                "Created scale %s/E%d/%d = %s".formatted(category.getCode(), request.echelon(), request.year(),
                        request.salaryMultiplier()));
        return SalaryScaleDto.from(saved);
    }

    @Transactional
    public SalaryScaleDto update(Long id, SalaryScaleRequest request) {
        SalaryScale scale = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary scale", id));
        validateYearOverYear(scale.getCategory().getId(), scale.getEchelon(), scale.getYear(),
                request.salaryMultiplier());
        apply(scale, request);
        auditService.log("SALARY_SCALE", id, "UPDATE", "Updated multiplier to " + request.salaryMultiplier());
        return SalaryScaleDto.from(scale);
    }

    @Transactional
    public void delete(Long id) {
        SalaryScale scale = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary scale", id));
        repository.delete(scale);
        auditService.log("SALARY_SCALE", id, "DELETE", "Deleted salary scale " + id);
    }

    /** The multiplier for a given cell must not be lower than the previous year's. */
    private void validateYearOverYear(Long categoryId, Integer echelon, Integer year, java.math.BigDecimal multiplier) {
        repository.findByCategoryIdAndEchelonAndYear(categoryId, echelon, year - 1)
                .ifPresent(previous -> {
                    if (multiplier.compareTo(previous.getSalaryMultiplier()) < 0) {
                        throw new IllegalArgumentException(
                                "Multiplier (%s) cannot be lower than the previous year's (%s)"
                                        .formatted(multiplier, previous.getSalaryMultiplier()));
                    }
                });
    }

    private void apply(SalaryScale scale, SalaryScaleRequest request) {
        scale.setEchelon(request.echelon());
        scale.setYear(request.year());
        scale.setSalaryMultiplier(request.salaryMultiplier());
        scale.setAnnualSalary(request.annualSalary());
        scale.setIncreaseAmount(request.increaseAmount());
        scale.setIncreasePercent(request.increasePercent());
        scale.setValidFrom(request.validFrom() != null ? request.validFrom() : LocalDate.of(request.year(), 1, 1));
        scale.setValidTo(request.validTo());
    }
}
