package com.maram.payroll.employee.controller;

import com.maram.payroll.employee.dto.DepartmentDto;
import com.maram.payroll.employee.dto.DepartmentRequest;
import com.maram.payroll.employee.service.DepartmentService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "Organisational structure")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "List all departments")
    public List<DepartmentDto> list() {
        return departmentService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "Get a department")
    public DepartmentDto get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Create a department")
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Update a department")
    public DepartmentDto update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config.manage')")
    @Operation(summary = "Delete a department")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
