package com.maram.payroll.reporting.controller;

import com.maram.payroll.reporting.service.AttendanceExportService;
import com.maram.payroll.reporting.service.PayrollExportService;
import com.maram.payroll.reporting.service.PayslipService;
import com.maram.payroll.reporting.service.TaxDeclarationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report downloads: payslip PDF, payroll Excel, tax-declaration CSV, attendance Excel.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Payslips, exports & tax declarations")
public class ReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv");

    private final PayslipService payslipService;
    private final PayrollExportService payrollExportService;
    private final TaxDeclarationService taxDeclarationService;
    private final AttendanceExportService attendanceExportService;

    public ReportController(PayslipService payslipService,
                            PayrollExportService payrollExportService,
                            TaxDeclarationService taxDeclarationService,
                            AttendanceExportService attendanceExportService) {
        this.payslipService = payslipService;
        this.payrollExportService = payrollExportService;
        this.taxDeclarationService = taxDeclarationService;
        this.attendanceExportService = attendanceExportService;
    }

    @GetMapping("/payslip/{payrollId}")
    @PreAuthorize("hasAuthority('payroll.view')")
    @Operation(summary = "Download a payslip PDF for a payroll record")
    public ResponseEntity<Resource> payslip(@PathVariable Long payrollId) {
        PayslipService.Payslip slip = payslipService.generate(payrollId);
        return download(slip.fileName(), slip.content(), MediaType.APPLICATION_PDF);
    }

    @GetMapping("/payroll/export")
    @PreAuthorize("hasAuthority('payroll.export')")
    @Operation(summary = "Download a period's payroll as Excel")
    public ResponseEntity<Resource> payrollExport(@RequestParam Long periodId) {
        PayrollExportService.Export export = payrollExportService.exportPeriod(periodId);
        return download(export.fileName(), export.content(), XLSX);
    }

    @GetMapping("/tax/irpp")
    @PreAuthorize("hasAuthority('report.export')")
    @Operation(summary = "Download the IRPP declaration CSV for a period")
    public ResponseEntity<Resource> irpp(@RequestParam Long periodId) {
        TaxDeclarationService.Declaration d = taxDeclarationService.irpp(periodId);
        return download(d.fileName(), d.content(), CSV);
    }

    @GetMapping("/tax/cnss")
    @PreAuthorize("hasAuthority('report.export')")
    @Operation(summary = "Download the CNSS declaration CSV for a period")
    public ResponseEntity<Resource> cnss(@RequestParam Long periodId) {
        TaxDeclarationService.Declaration d = taxDeclarationService.cnss(periodId);
        return download(d.fileName(), d.content(), CSV);
    }

    @GetMapping("/attendance/export")
    @PreAuthorize("hasAuthority('report.export')")
    @Operation(summary = "Download the monthly attendance report as Excel")
    public ResponseEntity<Resource> attendanceExport(@RequestParam int year,
                                                     @RequestParam int month,
                                                     @RequestParam(name = "department_id", required = false) Long departmentId) {
        AttendanceExportService.Export export = attendanceExportService.export(year, month, departmentId);
        return download(export.fileName(), export.content(), XLSX);
    }

    private ResponseEntity<Resource> download(String fileName, byte[] content, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(content));
    }
}
