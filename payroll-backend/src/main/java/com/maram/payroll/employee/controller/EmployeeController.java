package com.maram.payroll.employee.controller;

import com.maram.payroll.common.dto.PageResponse;
import com.maram.payroll.employee.dto.EmployeeCreateRequest;
import com.maram.payroll.employee.dto.EmployeeDto;
import com.maram.payroll.employee.dto.EmployeeUpdateRequest;
import com.maram.payroll.employee.dto.ImportPreviewResponse;
import com.maram.payroll.employee.service.EmployeeImportService;
import com.maram.payroll.employee.service.EmployeeService;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employees", description = "Employee master data management")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeImportService importService;

    public EmployeeController(EmployeeService employeeService, EmployeeImportService importService) {
        this.employeeService = employeeService;
        this.importService = importService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "List employees (paginated, filterable)")
    public PageResponse<EmployeeDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(name = "department_id", required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return employeeService.search(departmentId, status, search, page, limit);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "Get an employee by id")
    public EmployeeDto get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('employee.create')")
    @Operation(summary = "Create an employee")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.edit')")
    @Operation(summary = "Update an employee")
    public EmployeeDto update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.delete')")
    @Operation(summary = "Delete an employee")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('employee.import')")
    @Operation(summary = "Validate an .xlsx upload without writing (row-by-row preview)")
    public ImportPreviewResponse importPreview(@RequestParam("file") MultipartFile file) {
        return importService.preview(file);
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('employee.import')")
    @Operation(summary = "Import employees from an .xlsx upload (atomic — rejected if any row is invalid)")
    public ResponseEntity<?> importCommit(@RequestParam("file") MultipartFile file) {
        ImportPreviewResponse preview = importService.preview(file);
        if (preview.invalidRows() > 0) {
            // Return the per-row errors so the client can show what to fix.
            return ResponseEntity.badRequest().body(preview);
        }
        return ResponseEntity.ok(importService.commit(file));
    }
}
