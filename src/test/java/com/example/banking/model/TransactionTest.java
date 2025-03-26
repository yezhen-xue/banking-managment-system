package com.example.banking.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testDefaultConstructor() {
        Transaction transaction = new Transaction();
        
        assertNotNull(transaction.getId());
        assertNotNull(transaction.getTimestamp());
        assertNull(transaction.getAmount());
        assertNull(transaction.getDescription());
        assertNull(transaction.getType());
    }

    @Test
    void testParameterizedConstructor() {
        BigDecimal amount = new BigDecimal("100.50");
        String description = "测试交易";
        TransactionType type = TransactionType.INCOME;
        
        Transaction transaction = new Transaction(amount, description, type);
        
        assertNotNull(transaction.getId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(description, transaction.getDescription());
        assertEquals(type, transaction.getType());
        assertNotNull(transaction.getTimestamp());
    }

    @Test
    void testFullConstructor() {
        Long id = 1L;
        BigDecimal amount = new BigDecimal("100.50");
        String description = "测试交易";
        TransactionType type = TransactionType.INCOME;
        LocalDateTime timestamp = LocalDateTime.now();
        
        Transaction transaction = new Transaction(id, amount, description, type, timestamp);
        
        assertEquals(id, transaction.getId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(description, transaction.getDescription());
        assertEquals(type, transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
    }

    @Test
    void testFullConstructorWithNullTimestamp() {
        Long id = 1L;
        BigDecimal amount = new BigDecimal("100.50");
        String description = "测试交易";
        TransactionType type = TransactionType.INCOME;
        
        Transaction transaction = new Transaction(id, amount, description, type, null);
        
        assertEquals(id, transaction.getId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(description, transaction.getDescription());
        assertEquals(type, transaction.getType());
        assertNotNull(transaction.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        Transaction transaction1 = new Transaction(1L, new BigDecimal("100.50"), "交易1", TransactionType.INCOME, LocalDateTime.now());
        Transaction transaction2 = new Transaction(1L, new BigDecimal("200.75"), "交易2", TransactionType.EXPENSE, LocalDateTime.now());
        Transaction transaction3 = new Transaction(2L, new BigDecimal("100.50"), "交易1", TransactionType.INCOME, LocalDateTime.now());
        
        // 相同ID的交易应该相等
        assertEquals(transaction1, transaction2);
        assertEquals(transaction1.hashCode(), transaction2.hashCode());
        
        // 不同ID的交易不应该相等
        assertNotEquals(transaction1, transaction3);
        assertNotEquals(transaction1.hashCode(), transaction3.hashCode());
        
        // 与null或其他类型比较
        assertNotEquals(transaction1, null);
        assertNotEquals(transaction1, "not a transaction");
    }

    @Test
    void testToString() {
        Transaction transaction = new Transaction(1L, new BigDecimal("100.50"), "测试交易", TransactionType.INCOME, LocalDateTime.now());
        String toStringResult = transaction.toString();
        
        assertTrue(toStringResult.contains("id=1"));
        assertTrue(toStringResult.contains("amount=100.50"));
        assertTrue(toStringResult.contains("description='测试交易'"));
        assertTrue(toStringResult.contains("type=INCOME"));
        assertTrue(toStringResult.contains("timestamp="));
    }
} 