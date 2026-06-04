package com.maram.payroll.attendance.dto;

import java.util.List;

public record AttendanceImportPreview(
        int year,
        int month,
        int totalRows,
        int validRows,
        int invalidRows,
        int totalRecords,
        List<AttendanceImportRowResult> rows) {
}
