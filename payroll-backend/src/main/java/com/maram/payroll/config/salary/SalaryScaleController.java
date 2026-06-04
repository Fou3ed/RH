package com.maram.payroll.config.salary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/config/salary-scales")
@Tag(name = "Salary Scales", description = "Salary multiplier configuration by category/échelon/year")
public class SalaryScaleController {

    private final SalaryScaleService service;

    public SalaryScaleController(SalaryScaleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "List salary scales for a year")
    public List<SalaryScaleDto> list(@RequestParam(required = false) Integer year) {
        return service.listByYear(year != null ? year : LocalDate.now().getYear());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Create a salary scale entry")
    public ResponseEntity<SalaryScaleDto> create(@Valid @RequestBody SalaryScaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Update a salary scale entry")
    public SalaryScaleDto update(@PathVariable Long id, @Valid @RequestBody SalaryScaleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Delete a salary scale entry")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
