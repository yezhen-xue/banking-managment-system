package com.example.banking.dto;

import com.example.banking.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "交易数据传输对象")
public class TransactionDTO {
    @Schema(description = "交易ID", example = "1")
    private Long id;
    
    @Schema(description = "交易金额", example = "100.50")
    private BigDecimal amount;
    
    @Schema(description = "交易描述", example = "餐饮消费")
    private String description;
    
    @Schema(description = "交易类型", example = "EXPENSE")
    private TransactionType type;
    
    @Schema(description = "交易时间", example = "2023-06-15T14:30:00")
    private LocalDateTime timestamp;

    // 默认构造函数
    public TransactionDTO() {
    }

    // 带参数的构造函数
    public TransactionDTO(Long id, BigDecimal amount, String description, TransactionType type, LocalDateTime timestamp) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
} 