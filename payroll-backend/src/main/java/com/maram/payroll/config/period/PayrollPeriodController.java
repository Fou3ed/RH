package com.maram.payroll.config.period;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payroll-periods")
@Tag(name = "Payroll Periods", description = "Monthly payroll windows + lifecycle")
public class PayrollPeriodController {

    private final PayrollPeriodService service;

    public PayrollPeriodController(PayrollPeriodService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "List payroll periods (newest first)")
    public List<PayrollPeriodDtos.Response> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('config.manage','payroll.calculate')")
    @Operation(summary = "Create a payroll period (one per month/year)")
    public ResponseEntity<PayrollPeriodDtos.Response> create(
            @Valid @RequestBody PayrollPeriodDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyAuthority('config.manage','payroll.calculate','payroll.approve')")
    @Operation(summary = "Move a period to the next lifecycle status")
    public PayrollPeriodDtos.Response transition(@PathVariable Long id,
                                                 @Valid @RequestBody PayrollPeriodDtos.TransitionRequest request) {
        return service.transition(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Delete a DRAFT payroll period")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
