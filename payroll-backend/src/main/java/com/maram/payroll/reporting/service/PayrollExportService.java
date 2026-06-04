package com.maram.payroll.reporting.service;

import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.config.period.PayrollPeriod;
import com.maram.payroll.config.period.PayrollPeriodRepository;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Exports a period's calculated payroll as an .xlsx with a detailed sheet (one row
 * per employee) and a totals row.
 */
@Service
public class PayrollExportService {

    private static final String[] HEADERS = {
            "Employee ID", "Name", "Days worked", "Base", "Adjusted",
            "Presence", "Transport", "Diligence", "Meal", "Child", "Performance", "Total allowances",
            "Gross", "IRPP", "CNSS", "Health", "Total deductions", "Net", "Status"
    };

    private final PayrollRepository payrollRepository;
    private final PayrollPeriodRepository periodRepository;

    public PayrollExportService(PayrollRepository payrollRepository, PayrollPeriodRepository periodRepository) {
        this.payrollRepository = payrollRepository;
        this.periodRepository = periodRepository;
    }

    public record Export(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public Export exportPeriod(Long periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", periodId));
        List<Payroll> rows = payrollRepository.findByPayrollPeriodIdOrderByEmployee_FullNameAsc(periodId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Payroll " + period.getPeriodCode());

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
            BigDecimal totalGross = BigDecimal.ZERO, totalDed = BigDecimal.ZERO, totalNet = BigDecimal.ZERO;
            for (Payroll p : rows) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(p.getEmployee().getEmployeeId());
                row.createCell(c++).setCellValue(p.getEmployee().getFullName());
                c = num(row, c, p.getDaysWorked());
                c = num(row, c, p.getBaseSalary());
                c = num(row, c, p.getAdjustedSalary());
                c = num(row, c, p.getPresenceAllowance());
                c = num(row, c, p.getTransportAllowance());
                c = num(row, c, p.getDiligenceAllowance());
                c = num(row, c, p.getMealAllowance());
                c = num(row, c, p.getChildAllowance());
                c = num(row, c, p.getPerformanceBonus());
                c = num(row, c, p.getTotalAllowances());
                c = num(row, c, p.getGrossSalary());
                c = num(row, c, p.getIncomeTaxIrpp());
                c = num(row, c, p.getCnssContribution());
                c = num(row, c, p.getHealthInsurance());
                c = num(row, c, p.getTotalDeductions());
                c = num(row, c, p.getNetSalary());
                row.createCell(c).setCellValue(p.getPaymentStatus());

                totalGross = totalGross.add(nz(p.getGrossSalary()));
                totalDed = totalDed.add(nz(p.getTotalDeductions()));
                totalNet = totalNet.add(nz(p.getNetSalary()));
            }

            Row totals = sheet.createRow(r);
            Cell label = totals.createCell(11);
            label.setCellValue("TOTALS");
            label.setCellStyle(headerStyle);
            num(totals, 12, totalGross);
            num(totals, 16, totalDed);
            num(totals, 17, totalNet);

            for (int c = 0; c < HEADERS.length; c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(out);
            return new Export("payroll_%s.xlsx".formatted(period.getPeriodCode()), out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export payroll: " + e.getMessage(), e);
        }
    }

    private int num(Row row, int col, BigDecimal value) {
        row.createCell(col).setCellValue(nz(value).doubleValue());
        return col + 1;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
