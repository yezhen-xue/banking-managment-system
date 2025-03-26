package com.example.banking.repository;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    List<Transaction> findAll();

    boolean deleteById(Long id);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount);

    List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<Transaction> findWithPagination(int offset, int limit);

    long count();

    List<Transaction> findPotentialDuplicates(
            BigDecimal amount, 
            String description, 
            TransactionType type,
            int timeWindow);
} 