package com.maram.payroll.config.allowance;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD for company-wide allowance defaults. Small enough that the service logic
 * lives inline here.
 */
@RestController
@RequestMapping("/config/allowances")
@Tag(name = "Allowance Configuration", description = "Company-wide default allowances")
public class AllowanceConfigController {

    private final AllowanceConfigRepository repository;
    private final AuditService auditService;

    public AllowanceConfigController(AllowanceConfigRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "List allowance configuration")
    public List<AllowanceConfigDtos.Response> list() {
        return repository.findByOrderByAllowanceTypeAsc().stream()
                .map(AllowanceConfigDtos.Response::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config.manage')")
    @Transactional
    @Operation(summary = "Create an allowance default")
    public ResponseEntity<AllowanceConfigDtos.Response> create(@Valid @RequestBody AllowanceConfigDtos.Request request) {
        AllowanceConfig config = new AllowanceConfig();
        apply(config, request);
        AllowanceConfig saved = repository.save(config);
        auditService.log("ALLOWANCE_CONFIG", saved.getId(), "CREATE", "Created " + request.allowanceType());
        return ResponseEntity.status(HttpStatus.CREATED).body(AllowanceConfigDtos.Response.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Transactional
    @Operation(summary = "Update an allowance default")
    public AllowanceConfigDtos.Response update(@PathVariable Long id,
                                               @Valid @RequestBody AllowanceConfigDtos.Request request) {
        AllowanceConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allowance config", id));
        apply(config, request);
        auditService.log("ALLOWANCE_CONFIG", id, "UPDATE", "Updated " + request.allowanceType());
        return AllowanceConfigDtos.Response.from(config);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Transactional
    @Operation(summary = "Delete an allowance default")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        AllowanceConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allowance config", id));
        repository.delete(config);
        auditService.log("ALLOWANCE_CONFIG", id, "DELETE", "Deleted allowance config " + id);
        return ResponseEntity.noContent().build();
    }

    private void apply(AllowanceConfig config, AllowanceConfigDtos.Request request) {
        config.setAllowanceType(request.allowanceType().toUpperCase());
        config.setAmount(request.amount());
        config.setAttendanceAdjusted(request.attendanceAdjusted());
        config.setEffectiveDate(request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now());
        config.setEndDate(request.endDate());
    }
}
