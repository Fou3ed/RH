package com.maram.payroll.attendance.controller;

import com.maram.payroll.attendance.dto.AttendanceDto;
import com.maram.payroll.attendance.dto.AttendanceImportPreview;
import com.maram.payroll.attendance.dto.AttendanceReportRow;
import com.maram.payroll.attendance.dto.AttendanceRequest;
import com.maram.payroll.attendance.dto.AttendanceSummaryDto;
import com.maram.payroll.attendance.dto.AttendanceUpdateRequest;
import com.maram.payroll.attendance.service.AttendanceImportService;
import com.maram.payroll.attendance.service.AttendanceService;
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

import java.util.List;

@RestController
@RequestMapping("/attendance")
@Tag(name = "Attendance", description = "Daily attendance tracking, import & analytics")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceImportService importService;

    public AttendanceController(AttendanceService attendanceService, AttendanceImportService importService) {
        this.attendanceService = attendanceService;
        this.importService = importService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance.record')")
    @Operation(summary = "Record a single day's attendance")
    public ResponseEntity<AttendanceDto> record(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.record(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.record')")
    @Operation(summary = "Update an attendance record")
    public AttendanceDto update(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request) {
        return attendanceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance.record')")
    @Operation(summary = "Delete an attendance record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}/summary")
    @PreAuthorize("hasAuthority('attendance.view')")
    @Operation(summary = "Monthly attendance summary + day records for one employee")
    public AttendanceSummaryDto summary(@PathVariable Long employeeId,
                                        @RequestParam int year,
                                        @RequestParam int month) {
        return attendanceService.monthlySummary(employeeId, year, month);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAuthority('attendance.view')")
    @Operation(summary = "Attendance report — per-employee aggregates (worst attendance first)")
    public List<AttendanceReportRow> report(@RequestParam int year,
                                            @RequestParam int month,
                                            @RequestParam(name = "department_id", required = false) Long departmentId) {
        return attendanceService.report(year, month, departmentId);
    }

    @PostMapping(value = "/import/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('attendance.import')")
    @Operation(summary = "Validate a monthly attendance grid without writing")
    public AttendanceImportPreview importPreview(@RequestParam("file") MultipartFile file,
                                                 @RequestParam int year,
                                                 @RequestParam int month) {
        return importService.preview(file, year, month);
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('attendance.import')")
    @Operation(summary = "Import a monthly attendance grid (atomic — rejected if any row is invalid)")
    public ResponseEntity<?> importCommit(@RequestParam("file") MultipartFile file,
                                          @RequestParam int year,
                                          @RequestParam int month) {
        AttendanceImportPreview preview = importService.preview(file, year, month);
        if (preview.invalidRows() > 0) {
            return ResponseEntity.badRequest().body(preview);
        }
        return ResponseEntity.ok(importService.commit(file, year, month));
    }
}
