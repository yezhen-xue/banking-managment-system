package com.example.banking.exception;

public class BusinessException extends RuntimeException {
    
    private final int statusCode;
    private final String errorType;

    public BusinessException(String message, int statusCode, String errorType) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public BusinessException(String message, int statusCode) {
        this(message, statusCode, "Business Error");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }
} 