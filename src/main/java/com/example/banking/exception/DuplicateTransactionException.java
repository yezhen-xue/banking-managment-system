package com.example.banking.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
    
    public DuplicateTransactionException(String amount, String description, String type) {
        super(String.format("存在重复交易: 金额=%s, 描述=%s, 类型=%s", amount, description, type));
    }
} 