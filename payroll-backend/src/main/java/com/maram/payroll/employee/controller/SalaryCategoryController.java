package com.maram.payroll.employee.controller;

import com.maram.payroll.employee.dto.SalaryCategoryDto;
import com.maram.payroll.employee.repository.SalaryCategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only access to salary categories (reference data needed by the employee form).
 * Full management arrives in Sprint 5 (Payroll Configuration).
 */
@RestController
@RequestMapping("/salary-categories")
@Tag(name = "Salary Categories", description = "Reference data — salary bands")
public class SalaryCategoryController {

    private final SalaryCategoryRepository repository;

    public SalaryCategoryController(SalaryCategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "List salary categories")
    public List<SalaryCategoryDto> list() {
        return repository.findAll().stream()
                .map(c -> new SalaryCategoryDto(c.getId(), c.getCode(), c.getName()))
                .toList();
    }
}
