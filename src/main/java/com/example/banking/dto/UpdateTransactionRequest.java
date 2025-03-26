package com.example.banking.dto;

import com.example.banking.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "更新交易的请求对象")
public class UpdateTransactionRequest {
    @Schema(description = "交易金额", example = "120.75", required = true)
    private BigDecimal amount;
    
    @Schema(description = "交易描述", example = "更新后的餐饮消费", required = true)
    private String description;
    
    @Schema(description = "交易类型", example = "EXPENSE", required = true)
    private TransactionType type;

    // 默认构造函数
    public UpdateTransactionRequest() {
    }

    // 带参数的构造函数
    public UpdateTransactionRequest(BigDecimal amount, String description, TransactionType type) {
        this.amount = amount;
        this.description = description;
        this.type = type;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
} 