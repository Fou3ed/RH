package com.maram.payroll.attendance.service;

import com.maram.payroll.attendance.dto.AttendanceImportCommit;
import com.maram.payroll.attendance.dto.AttendanceImportPreview;
import com.maram.payroll.attendance.dto.AttendanceImportRowResult;
import com.maram.payroll.attendance.entity.Attendance;
import com.maram.payroll.attendance.entity.AttendanceStatus;
import com.maram.payroll.attendance.repository.AttendanceRepository;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Imports a monthly attendance grid: column 0 is {@code employeeId}, the remaining
 * header cells are day numbers (1..31), and each cell holds a status code
 * (8=present, 4=half-day, A=absent, C=leave, H=holiday, W=weekend; blank = no record).
 * The target month is supplied by the caller. Commit is atomic.
 */
@Service
public class AttendanceImportService {

    private static final Map<String, AttendanceStatus> CODE_MAP = Map.of(
            "8", AttendanceStatus.PRESENT,
            "7", AttendanceStatus.PRESENT,
            "4", AttendanceStatus.HALF_DAY,
            "A", AttendanceStatus.ABSENT,
            "C", AttendanceStatus.LEAVE,
            "H", AttendanceStatus.HOLIDAY,
            "W", AttendanceStatus.WEEKEND);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public AttendanceImportService(AttendanceRepository attendanceRepository,
                                   EmployeeRepository employeeRepository,
                                   AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AttendanceImportPreview preview(MultipartFile file, int year, int month) {
        return build(parse(file, year, month), year, month);
    }

    @Transactional
    public AttendanceImportCommit commit(MultipartFile file, int year, int month) {
        List<ParsedRow> rows = parse(file, year, month);
        long invalid = rows.stream().filter(r -> !r.errors.isEmpty()).count();
        if (invalid > 0) {
            throw new IllegalArgumentException(
                    "Import rejected: " + invalid + " invalid row(s). Use the preview endpoint for details.");
        }

        List<Attendance> toSave = new ArrayList<>();
        for (ParsedRow row : rows) {
            for (DayEntry entry : row.entries) {
                Attendance a = new Attendance();
                a.setEmployee(row.employee);
                a.setAttendanceDate(entry.date);
                a.setDayOfWeek(entry.date.getDayOfWeek().name());
                a.setAttendanceStatus(entry.status);
                a.setAttendanceCode(entry.code);
                a.setDaysFraction(entry.status.dayFraction());
                a.setPaidLeave(entry.status == AttendanceStatus.LEAVE);
                toSave.add(a);
            }
        }
        attendanceRepository.saveAll(toSave);
        auditService.log("ATTENDANCE", 0L, "IMPORT",
                "Imported %d attendance records for %d-%02d".formatted(toSave.size(), year, month));
        return new AttendanceImportCommit(toSave.size(),
                "Imported %d attendance records".formatted(toSave.size()));
    }

    // ---- parsing & validation ----

    private List<ParsedRow> parse(MultipartFile file, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);

        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new IllegalArgumentException("The uploaded file has no header row.");
            }

            // Map column index -> day-of-month (skip column 0 which is the employee id).
            Map<Integer, Integer> dayColumns = new HashMap<>();
            for (Cell cell : header) {
                if (cell.getColumnIndex() == 0) {
                    continue;
                }
                Integer day = parseDay(cellToString(cell));
                if (day != null && day >= 1 && day <= ym.lengthOfMonth()) {
                    dayColumns.put(cell.getColumnIndex(), day);
                }
            }
            if (dayColumns.isEmpty()) {
                throw new IllegalArgumentException("No valid day columns (1.." + ym.lengthOfMonth() + ") found in the header.");
            }

            // Resolve employees once.
            Map<String, Employee> employees = new HashMap<>();
            employeeRepository.findAll().forEach(e ->
                    employees.put(e.getEmployeeId().toLowerCase(Locale.ROOT), e));

            Set<String> seenEmployeeIds = new HashSet<>();
            List<ParsedRow> rows = new ArrayList<>();

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String employeeId = cellToString(row.getCell(0));
                if (employeeId == null || employeeId.isBlank()) {
                    continue; // skip blank rows
                }

                ParsedRow parsed = new ParsedRow(i, employeeId);

                Employee employee = employees.get(employeeId.trim().toLowerCase(Locale.ROOT));
                if (employee == null) {
                    parsed.errors.add("Unknown employee ID: " + employeeId);
                } else {
                    parsed.employee = employee;
                    if (!seenEmployeeIds.add(employeeId.trim().toLowerCase(Locale.ROOT))) {
                        parsed.errors.add("Duplicate employee row in file: " + employeeId);
                    }
                }

                for (Map.Entry<Integer, Integer> col : dayColumns.entrySet()) {
                    String raw = cellToString(row.getCell(col.getKey()));
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String code = raw.trim().toUpperCase(Locale.ROOT);
                    AttendanceStatus status = CODE_MAP.get(code);
                    if (status == null) {
                        parsed.errors.add("Day %d: unknown code '%s'".formatted(col.getValue(), raw));
                        continue;
                    }
                    LocalDate date = ym.atDay(col.getValue());
                    if (employee != null
                            && attendanceRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), date)) {
                        parsed.errors.add("Day %d: attendance already exists".formatted(col.getValue()));
                        continue;
                    }
                    parsed.entries.add(new DayEntry(date, status, code));
                }
                rows.add(parsed);
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        }
    }

    private AttendanceImportPreview build(List<ParsedRow> rows, int year, int month) {
        List<AttendanceImportRowResult> results = rows.stream()
                .map(r -> new AttendanceImportRowResult(
                        r.rowNumber, r.employeeId, r.entries.size(), r.errors.isEmpty(), List.copyOf(r.errors)))
                .toList();
        int valid = (int) results.stream().filter(AttendanceImportRowResult::valid).count();
        int totalRecords = rows.stream().filter(r -> r.errors.isEmpty()).mapToInt(r -> r.entries.size()).sum();
        return new AttendanceImportPreview(
                year, month, results.size(), valid, results.size() - valid, totalRecords, results);
    }

    private static Integer parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private static final class ParsedRow {
        final int rowNumber;
        final String employeeId;
        final List<String> errors = new ArrayList<>();
        final List<DayEntry> entries = new ArrayList<>();
        Employee employee;

        ParsedRow(int rowNumber, String employeeId) {
            this.rowNumber = rowNumber;
            this.employeeId = employeeId;
        }
    }

    private record DayEntry(LocalDate date, AttendanceStatus status, String code) {
    }
}
