package com.maram.payroll.attendance.dto;

import java.util.List;

/**
 * Validation outcome for one employee row in a monthly attendance grid.
 *
 * @param recognizedDays number of non-blank day cells that parsed to a status
 */
public record AttendanceImportRowResult(
        int rowNumber,
        String employeeId,
        int recognizedDays,
        boolean valid,
        List<String> errors) {
}
