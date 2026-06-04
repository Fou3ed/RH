package com.maram.payroll.common.exception;

/**
 * Raised when a request conflicts with existing state (e.g. duplicate username).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
