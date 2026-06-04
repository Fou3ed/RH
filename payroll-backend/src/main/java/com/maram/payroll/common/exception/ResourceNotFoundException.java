package com.maram.payroll.common.exception;

/**
 * Raised when a requested entity does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Object id) {
        super("%s not found: %s".formatted(entity, id));
    }
}
