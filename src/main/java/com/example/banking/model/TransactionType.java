package com.example.banking.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "交易类型")
public enum TransactionType {
    @Schema(description = "收入类型交易")
    INCOME("收入"),
    
    @Schema(description = "支出类型交易")
    EXPENSE("支出"),
    
    @Schema(description = "转账类型交易")
    TRANSFER("转账");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 