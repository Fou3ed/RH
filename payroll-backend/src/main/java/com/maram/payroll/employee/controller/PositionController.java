package com.maram.payroll.employee.controller;

import com.maram.payroll.employee.dto.PositionDto;
import com.maram.payroll.employee.dto.PositionRequest;
import com.maram.payroll.employee.service.PositionService;
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

import java.util.List;

@RestController
@RequestMapping("/positions")
@Tag(name = "Positions", description = "Job titles / classifications")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "List positions (optionally filtered by department)")
    public List<PositionDto> list(@RequestParam(name = "department_id", required = false) Long departmentId) {
        return positionService.list(departmentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "Get a position")
    public PositionDto get(@PathVariable Long id) {
        return positionService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Create a position")
    public ResponseEntity<PositionDto> create(@Valid @RequestBody PositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(positionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Update a position")
    public PositionDto update(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
        return positionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Delete a position")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
