package com.maram.payroll.config.tax;

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
@RequestMapping("/config/tax")
@Tag(name = "Tax Configuration", description = "IRPP brackets, CNSS & health rates")
public class TaxConfigurationController {

    private final TaxConfigurationService service;

    public TaxConfigurationController(TaxConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "List tax configuration for a year")
    public List<TaxConfigurationDto> list(@RequestParam(required = false) Integer year) {
        return service.listByYear(year != null ? year : LocalDate.now().getYear());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Create a tax rule/bracket")
    public ResponseEntity<TaxConfigurationDto> create(@Valid @RequestBody TaxConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Update a tax rule/bracket")
    public TaxConfigurationDto update(@PathVariable Long id, @Valid @RequestBody TaxConfigurationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Delete a tax rule/bracket")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
