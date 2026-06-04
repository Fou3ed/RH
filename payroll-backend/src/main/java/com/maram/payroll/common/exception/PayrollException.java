package com.maram.payroll.common.exception;

/**
 * Raised when a payroll business rule is violated (e.g. missing salary scale,
 * calculation precondition not met).
 */
public class PayrollException extends RuntimeException {

    public PayrollException(String message) {
        super(message);
    }

    public PayrollException(String message, Throwable cause) {
        super(message, cause);
    }
}
