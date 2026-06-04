package com.maram.payroll.employee.dto;

/**
 * Result of a successful (atomic) import commit.
 */
public record ImportCommitResponse(
        int imported,
        String message) {
}
