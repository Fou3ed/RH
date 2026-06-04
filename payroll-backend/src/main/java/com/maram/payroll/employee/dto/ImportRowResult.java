package com.maram.payroll.employee.dto;

import java.util.List;

/**
 * Outcome of validating a single import row.
 *
 * @param rowNumber  1-based spreadsheet row (excluding the header)
 * @param employeeId the parsed employee id (may be blank if missing)
 * @param fullName   first + last name as parsed
 * @param valid      true when the row has no validation errors
 * @param errors     human-readable validation messages
 */
public record ImportRowResult(
        int rowNumber,
        String employeeId,
        String fullName,
        boolean valid,
        List<String> errors) {
}
