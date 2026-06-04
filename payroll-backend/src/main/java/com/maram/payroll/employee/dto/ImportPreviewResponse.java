package com.maram.payroll.employee.dto;

import java.util.List;

/**
 * Result of a dry-run import: row-by-row validation with no database writes.
 */
public record ImportPreviewResponse(
        int totalRows,
        int validRows,
        int invalidRows,
        List<ImportRowResult> rows) {
}
