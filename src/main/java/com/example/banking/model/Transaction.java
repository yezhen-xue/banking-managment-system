package com.example.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Transaction {
    // 使用原子长整型来生成唯一ID
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id;
    private BigDecimal amount;
    private String description;
    private TransactionType type;
    private LocalDateTime timestamp;

    // 默认构造函数
    public Transaction() {
        this.id = ID_GENERATOR.getAndIncrement();
        this.timestamp = LocalDateTime.now();
    }

    // 带参数的构造函数
    public Transaction(BigDecimal amount, String description, TransactionType type) {
        this();
        this.amount = amount;
        this.description = description;
        this.type = type;
    }

    // 用于克隆或手动设置ID的构造函数
    public Transaction(Long id, BigDecimal amount, String description, TransactionType type, LocalDateTime timestamp) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
} 