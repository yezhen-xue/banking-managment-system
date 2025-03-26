package com.example.banking.service;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.exception.BusinessException;
import com.example.banking.exception.DuplicateTransactionException;
import com.example.banking.exception.TransactionNotFoundException;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Value("${transaction.duplicate.timewindow:5}")
    private int duplicateTimeWindow;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionDTO createTransaction(CreateTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("交易金额必须大于零", HttpStatus.BAD_REQUEST.value(), "Invalid Amount");
        }
        
        // 检查重复交易
        List<Transaction> duplicates = transactionRepository.findPotentialDuplicates(
                request.getAmount(),
                request.getDescription(),
                request.getType(),
                duplicateTimeWindow
        );
        
        // 如果找到潜在的重复交易，抛出异常
        if (!duplicates.isEmpty()) {
            throw new DuplicateTransactionException(
                    request.getAmount().toString(),
                    request.getDescription(),
                    request.getType().toString()
            );
        }
        
        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getDescription(),
                request.getType()
        );
        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToDTO(savedTransaction);
    }

    @Override
    @Cacheable(value = "allTransactions")
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "transactions", key = "#id")
    public TransactionDTO getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Override
    @CacheEvict(value = {"transactions", "allTransactions"}, allEntries = true)
    public TransactionDTO updateTransaction(Long id, UpdateTransactionRequest request) {
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("交易金额必须大于零", HttpStatus.BAD_REQUEST.value(), "Invalid Amount");
        }
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToDTO(updatedTransaction);
    }

    @Override
    @CacheEvict(value = {"transactions", "allTransactions"}, allEntries = true)
    public void deleteTransaction(Long id) {
        if (!transactionRepository.deleteById(id)) {
            throw new TransactionNotFoundException(id);
        }
    }

    @Override
    public List<TransactionDTO> getTransactionsByType(TransactionType type) {
        if (type == null) {
            throw new BusinessException("交易类型不能为空", HttpStatus.BAD_REQUEST.value(), "Invalid Type");
        }
        
        return transactionRepository.findByType(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BusinessException("最小金额不能大于最大金额", HttpStatus.BAD_REQUEST.value(), "Invalid Amount Range");
        }
        
        return transactionRepository.findByAmountRange(minAmount, maxAmount).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST.value(), "Invalid Date Range");
        }
        
        return transactionRepository.findByDateRange(startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getTransactionsWithPagination(int page, int size) {
        if (page < 0) {
            throw new BusinessException("页码不能为负数", HttpStatus.BAD_REQUEST.value(), "Invalid Page");
        }
        
        if (size <= 0) {
            throw new BusinessException("每页大小必须大于零", HttpStatus.BAD_REQUEST.value(), "Invalid Size");
        }
        
        int offset = page * size;
        return transactionRepository.findWithPagination(offset, size).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getTransactionCount() {
        return transactionRepository.count();
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getType(),
                transaction.getTimestamp()
        );
    }
} 