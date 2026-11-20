package com.org.expense.exception;

/** 404 with code EXPENSE_NOT_FOUND. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
