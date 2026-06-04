package com.maram.payroll.reporting.service;

import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.payroll.entity.Payroll;
import com.maram.payroll.payroll.repository.PayrollRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Renders a single-page payslip PDF for a calculated payroll record (Apache PDFBox).
 */
@Service
public class PayslipService {

    private final PayrollRepository payrollRepository;

    public PayslipService(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    public record Payslip(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public Payslip generate(Long payrollId) {
        Payroll p = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", payrollId));

        String periodCode = p.getPayrollPeriod().getPeriodCode();
        String fileName = "payslip_%s_%s.pdf".formatted(p.getEmployee().getEmployeeId(), periodCode);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float left = 60;
                float y = 780;

                y = line(cs, PDType1Font.HELVETICA_BOLD, 18, left, y, "MARAM Confection — Payslip");
                y = line(cs, PDType1Font.HELVETICA, 11, left, y - 4, "Period: " + periodCode);
                y -= 10;
                y = line(cs, PDType1Font.HELVETICA_BOLD, 12, left, y, p.getEmployee().getFullName()
                        + "  (" + p.getEmployee().getEmployeeId() + ")");
                y = line(cs, PDType1Font.HELVETICA, 10, left, y,
                        "Days worked: " + p.getDaysWorked() + "    Status: " + p.getPaymentStatus());
                y -= 14;

                y = section(cs, left, y, "Earnings");
                y = row(cs, left, y, "Base salary (adjusted)", p.getAdjustedSalary());
                y = optionalRow(cs, left, y, "Presence allowance", p.getPresenceAllowance());
                y = optionalRow(cs, left, y, "Transport allowance", p.getTransportAllowance());
                y = optionalRow(cs, left, y, "Diligence allowance", p.getDiligenceAllowance());
                y = optionalRow(cs, left, y, "Meal allowance", p.getMealAllowance());
                y = optionalRow(cs, left, y, "Child allowance", p.getChildAllowance());
                y = optionalRow(cs, left, y, "Performance bonus", p.getPerformanceBonus());
                y = totalRow(cs, left, y, "Gross salary", p.getGrossSalary());
                y -= 14;

                y = section(cs, left, y, "Deductions");
                y = optionalRow(cs, left, y, "Income tax (IRPP)", p.getIncomeTaxIrpp());
                y = optionalRow(cs, left, y, "CNSS contribution", p.getCnssContribution());
                y = optionalRow(cs, left, y, "Health insurance", p.getHealthInsurance());
                y = totalRow(cs, left, y, "Total deductions", p.getTotalDeductions());
                y -= 18;

                totalRow(cs, left, y, "NET SALARY (TND)", p.getNetSalary());
            }

            doc.save(out);
            return new Payslip(fileName, out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate payslip: " + e.getMessage(), e);
        }
    }

    // ---- drawing helpers (return the new y position) ----

    private float line(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - (size + 6);
    }

    private float section(PDPageContentStream cs, float x, float y, String title) throws IOException {
        return line(cs, PDType1Font.HELVETICA_BOLD, 12, x, y, title);
    }

    private float row(PDPageContentStream cs, float x, float y, String label, BigDecimal amount) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x + 10, y);
        cs.showText(label);
        cs.endText();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x + 360, y);
        cs.showText(format(amount));
        cs.endText();
        return y - 18;
    }

    private float optionalRow(PDPageContentStream cs, float x, float y, String label, BigDecimal amount)
            throws IOException {
        if (amount == null || amount.signum() == 0) {
            return y;
        }
        return row(cs, x, y, label, amount);
    }

    private float totalRow(PDPageContentStream cs, float x, float y, String label, BigDecimal amount)
            throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x + 10, y);
        cs.showText(label);
        cs.endText();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x + 360, y);
        cs.showText(format(amount));
        cs.endText();
        return y - 18;
    }

    private String format(BigDecimal amount) {
        BigDecimal v = amount == null ? BigDecimal.ZERO : amount;
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
