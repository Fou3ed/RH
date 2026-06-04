package com.maram.payroll.reporting.service;

import com.maram.payroll.attendance.dto.AttendanceReportRow;
import com.maram.payroll.attendance.service.AttendanceService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Exports the monthly attendance report as an .xlsx.
 */
@Service
public class AttendanceExportService {

    private static final String[] HEADERS = {
            "Employee ID", "Name", "Department", "Present", "Half-days", "Absent", "Leave",
            "Days worked", "Attendance rate"
    };

    private final AttendanceService attendanceService;

    public AttendanceExportService(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    public record Export(String fileName, byte[] content) {
    }

    public Export export(int year, int month, Long departmentId) {
        List<AttendanceReportRow> rows = attendanceService.report(year, month, departmentId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Attendance %d-%02d".formatted(year, month));

            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (AttendanceReportRow row : rows) {
                Row sr = sheet.createRow(r++);
                sr.createCell(0).setCellValue(row.employeeCode());
                sr.createCell(1).setCellValue(row.employeeName());
                sr.createCell(2).setCellValue(row.departmentName() != null ? row.departmentName() : "");
                sr.createCell(3).setCellValue(row.presentDays());
                sr.createCell(4).setCellValue(row.halfDays());
                sr.createCell(5).setCellValue(row.absentDays());
                sr.createCell(6).setCellValue(row.leaveDays());
                sr.createCell(7).setCellValue(row.daysWorked().doubleValue());
                sr.createCell(8).setCellValue(row.attendanceRate().doubleValue());
            }

            for (int c = 0; c < HEADERS.length; c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(out);
            return new Export("attendance_%d-%02d.xlsx".formatted(year, month), out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export attendance: " + e.getMessage(), e);
        }
    }
}
