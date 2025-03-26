package com.example.banking.service;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionDTO createTransaction(CreateTransactionRequest request);

    List<TransactionDTO> getAllTransactions();

    TransactionDTO getTransactionById(Long id);

    TransactionDTO updateTransaction(Long id, UpdateTransactionRequest request);

    void deleteTransaction(Long id);

    List<TransactionDTO> getTransactionsByType(TransactionType type);

    List<TransactionDTO> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount);

    List<TransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<TransactionDTO> getTransactionsWithPagination(int page, int size);

    long getTransactionCount();
} 