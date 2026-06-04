package com.maram.payroll.employee.service;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.employee.dto.ImportCommitResponse;
import com.maram.payroll.employee.dto.ImportPreviewResponse;
import com.maram.payroll.employee.dto.ImportRowResult;
import com.maram.payroll.employee.entity.Department;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.entity.SalaryCategory;
import com.maram.payroll.employee.repository.DepartmentRepository;
import com.maram.payroll.employee.repository.EmployeeRepository;
import com.maram.payroll.employee.repository.SalaryCategoryRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Imports employees from an .xlsx workbook (Apache POI). The first sheet's first
 * row is the header; recognised columns are listed in {@link #REQUIRED_COLUMNS} and
 * the optional set. Validation is performed row-by-row and reported with line
 * numbers; {@link #commit} only writes when every row is valid (all-or-nothing).
 */
@Service
public class EmployeeImportService {

    static final List<String> REQUIRED_COLUMNS =
            List.of("employeeid", "firstname", "lastname", "hiredate", "departmentcode", "categorycode");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SalaryCategoryRepository categoryRepository;
    private final AuditService auditService;

    public EmployeeImportService(EmployeeRepository employeeRepository,
                                 DepartmentRepository departmentRepository,
                                 SalaryCategoryRepository categoryRepository,
                                 AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    /** Dry run — validates and reports, never writes. */
    @Transactional(readOnly = true)
    public ImportPreviewResponse preview(MultipartFile file) {
        List<ParsedRow> rows = parse(file);
        validate(rows);
        return summarise(rows);
    }

    /** Atomic import — writes only if every row is valid, otherwise throws. */
    @Transactional
    public ImportCommitResponse commit(MultipartFile file) {
        List<ParsedRow> rows = parse(file);
        validate(rows);

        long invalid = rows.stream().filter(r -> !r.errors.isEmpty()).count();
        if (invalid > 0) {
            throw new IllegalArgumentException(
                    "Import rejected: " + invalid + " invalid row(s). Use the preview endpoint for details.");
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file.");
        }

        List<Employee> employees = rows.stream().map(this::toEmployee).toList();
        employeeRepository.saveAll(employees);
        auditService.log("EMPLOYEE", 0L, "IMPORT", "Imported " + employees.size() + " employees");
        return new ImportCommitResponse(employees.size(), "Imported " + employees.size() + " employees");
    }

    // ---- parsing ----

    private List<ParsedRow> parse(MultipartFile file) {
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new IllegalArgumentException("The uploaded file has no header row.");
            }

            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : header) {
                String name = cellToString(cell);
                if (name != null && !name.isBlank()) {
                    columns.put(name.trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
                }
            }

            List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !columns.containsKey(c)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Missing required column(s): " + String.join(", ", missing));
            }

            List<ParsedRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = sheet.getFirstRowNum() + 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                ParsedRow parsed = new ParsedRow(i); // 0-based POI index; header is row 0
                parsed.employeeId = value(row, columns, "employeeid");
                parsed.firstName = value(row, columns, "firstname");
                parsed.lastName = value(row, columns, "lastname");
                parsed.hireDate = value(row, columns, "hiredate");
                parsed.departmentCode = value(row, columns, "departmentcode");
                parsed.categoryCode = value(row, columns, "categorycode");
                parsed.echelon = value(row, columns, "echelon");
                parsed.gender = value(row, columns, "gender");
                parsed.email = value(row, columns, "email");
                parsed.cnssNumber = value(row, columns, "cnssnumber");
                parsed.nationalId = value(row, columns, "nationalid");
                parsed.phoneNumber = value(row, columns, "phonenumber");
                parsed.baseSalary = value(row, columns, "basesalary");
                parsed.familyStatus = value(row, columns, "familystatus");
                parsed.numberOfChildren = value(row, columns, "numberofchildren");
                rows.add(parsed);
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        }
    }

    // ---- validation ----

    private void validate(List<ParsedRow> rows) {
        Map<String, Department> departments = new HashMap<>();
        departmentRepository.findAll().forEach(d -> departments.put(d.getCode().toLowerCase(Locale.ROOT), d));
        Map<String, SalaryCategory> categories = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categories.put(c.getCode().toLowerCase(Locale.ROOT), c));

        Set<String> seenEmployeeIds = new HashSet<>();
        Set<String> seenCnss = new HashSet<>();

        for (ParsedRow row : rows) {
            require(row, "Employee ID", row.employeeId);
            require(row, "First name", row.firstName);
            require(row, "Last name", row.lastName);
            require(row, "Department code", row.departmentCode);
            require(row, "Category code", row.categoryCode);

            // Hire date
            if (isBlank(row.hireDate)) {
                row.errors.add("Hire date is required");
            } else {
                try {
                    row.parsedHireDate = LocalDate.parse(row.hireDate.trim());
                    if (row.parsedHireDate.isAfter(LocalDate.now())) {
                        row.errors.add("Hire date cannot be in the future");
                    }
                } catch (Exception e) {
                    row.errors.add("Hire date is not a valid date (expected YYYY-MM-DD): " + row.hireDate);
                }
            }

            // Department / category resolution
            if (!isBlank(row.departmentCode)) {
                row.department = departments.get(row.departmentCode.trim().toLowerCase(Locale.ROOT));
                if (row.department == null) {
                    row.errors.add("Unknown department code: " + row.departmentCode);
                }
            }
            if (!isBlank(row.categoryCode)) {
                row.category = categories.get(row.categoryCode.trim().toLowerCase(Locale.ROOT));
                if (row.category == null) {
                    row.errors.add("Unknown category code: " + row.categoryCode);
                }
            }

            // Optional formats
            if (!isBlank(row.gender) && !row.gender.trim().matches("[MH]")) {
                row.errors.add("Gender must be M or H");
            }
            if (!isBlank(row.echelon)) {
                Integer e = parseInt(row.echelon);
                if (e == null || e < 1 || e > 14) {
                    row.errors.add("Échelon must be a whole number between 1 and 14");
                }
            }
            if (!isBlank(row.numberOfChildren) && parseInt(row.numberOfChildren) == null) {
                row.errors.add("Number of children must be a whole number");
            }
            if (!isBlank(row.baseSalary) && parseDecimal(row.baseSalary) == null) {
                row.errors.add("Base salary must be a number");
            }
            if (!isBlank(row.email) && !row.email.contains("@")) {
                row.errors.add("Email is not valid");
            }

            // Uniqueness — within the file and against the database
            if (!isBlank(row.employeeId)) {
                String key = row.employeeId.trim().toLowerCase(Locale.ROOT);
                if (!seenEmployeeIds.add(key)) {
                    row.errors.add("Duplicate employee ID within the file: " + row.employeeId);
                } else if (employeeRepository.existsByEmployeeId(row.employeeId.trim())) {
                    row.errors.add("Employee ID already exists: " + row.employeeId);
                }
            }
            if (!isBlank(row.cnssNumber)) {
                String key = row.cnssNumber.trim().toLowerCase(Locale.ROOT);
                if (!seenCnss.add(key)) {
                    row.errors.add("Duplicate CNSS number within the file: " + row.cnssNumber);
                } else if (employeeRepository.existsByCnssNumber(row.cnssNumber.trim())) {
                    row.errors.add("CNSS number already exists: " + row.cnssNumber);
                }
            }
        }
    }

    private ImportPreviewResponse summarise(List<ParsedRow> rows) {
        List<ImportRowResult> results = rows.stream()
                .map(r -> new ImportRowResult(
                        r.lineNumber(),
                        r.employeeId,
                        fullName(r),
                        r.errors.isEmpty(),
                        List.copyOf(r.errors)))
                .toList();
        int valid = (int) results.stream().filter(ImportRowResult::valid).count();
        return new ImportPreviewResponse(results.size(), valid, results.size() - valid, results);
    }

    private Employee toEmployee(ParsedRow row) {
        Employee e = new Employee();
        e.setEmployeeId(row.employeeId.trim());
        e.setFirstName(row.firstName.trim());
        e.setLastName(row.lastName.trim());
        e.setFullName(fullName(row));
        e.setHireDate(row.parsedHireDate);
        e.setDepartment(row.department);
        e.setCategory(row.category);
        e.setEchelon(parseInt(row.echelon));
        e.setGender(isBlank(row.gender) ? null : row.gender.trim());
        e.setEmail(trimToNull(row.email));
        e.setCnssNumber(trimToNull(row.cnssNumber));
        e.setNationalId(trimToNull(row.nationalId));
        e.setPhoneNumber(trimToNull(row.phoneNumber));
        e.setBaseSalary(parseDecimal(row.baseSalary));
        e.setFamilyStatus(trimToNull(row.familyStatus));
        e.setNumberOfChildren(parseInt(row.numberOfChildren));
        e.setEmploymentStatus("ACTIVE");
        return e;
    }

    // ---- cell helpers ----

    private static void require(ParsedRow row, String label, String value) {
        if (isBlank(value)) {
            row.errors.add(label + " is required");
        }
    }

    private static String value(Row row, Map<String, Integer> columns, String column) {
        Integer idx = columns.get(column);
        if (idx == null) {
            return null;
        }
        return cellToString(row.getCell(idx));
    }

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case FORMULA -> cell.getCellType() == CellType.FORMULA ? safeFormula(cell) : null;
            default -> null;
        };
    }

    private static String safeFormula(Cell cell) {
        try {
            return cell.getStringCellValue().trim();
        } catch (Exception ignored) {
            return String.valueOf(cell.getNumericCellValue());
        }
    }

    private static boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cellToString(cell) != null && !cellToString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String fullName(ParsedRow row) {
        String first = row.firstName == null ? "" : row.firstName.trim();
        String last = row.lastName == null ? "" : row.lastName.trim();
        return (first + " " + last).trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static Integer parseInt(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Mutable parse holder for a single data row. */
    private static final class ParsedRow {
        final int poiRowIndex;
        final List<String> errors = new ArrayList<>();

        String employeeId, firstName, lastName, hireDate, departmentCode, categoryCode;
        String echelon, gender, email, cnssNumber, nationalId, phoneNumber, baseSalary, familyStatus, numberOfChildren;

        LocalDate parsedHireDate;
        Department department;
        SalaryCategory category;

        ParsedRow(int poiRowIndex) {
            this.poiRowIndex = poiRowIndex;
        }

        /** 1-based data line number (excludes the header row). */
        int lineNumber() {
            return poiRowIndex;
        }
    }
}
