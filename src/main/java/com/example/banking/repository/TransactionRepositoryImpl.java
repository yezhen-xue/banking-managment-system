package com.example.banking.repository;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    // 使用ConcurrentHashMap保证线程安全
    private final Map<Long, Transaction> transactionsMap = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        // 如果是新交易（没有ID），将自动在Transaction构造函数中分配ID
        transactionsMap.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(transactionsMap.get(id));
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactionsMap.values());
    }

    @Override
    public boolean deleteById(Long id) {
        return transactionsMap.remove(id) != null;
    }

    @Override
    public List<Transaction> findByType(TransactionType type) {
        return transactionsMap.values().stream()
                .filter(transaction -> transaction.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionsMap.values().stream()
                .filter(transaction -> {
                    BigDecimal amount = transaction.getAmount();
                    return (minAmount == null || amount.compareTo(minAmount) >= 0) &&
                           (maxAmount == null || amount.compareTo(maxAmount) <= 0);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionsMap.values().stream()
                .filter(transaction -> {
                    LocalDateTime date = transaction.getTimestamp();
                    return (startDate == null || date.isEqual(startDate) || date.isAfter(startDate)) &&
                           (endDate == null || date.isEqual(endDate) || date.isBefore(endDate));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findWithPagination(int offset, int limit) {
        return transactionsMap.values().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return transactionsMap.size();
    }
    
    @Override
    public List<Transaction> findPotentialDuplicates(
            BigDecimal amount, 
            String description, 
            TransactionType type,
            int timeWindow) {
        
        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 时间窗口开始时间
        LocalDateTime windowStart = now.minusMinutes(timeWindow);
        
        // 查找满足条件的潜在重复交易
        return transactionsMap.values().stream()
                .filter(transaction -> 
                    // 金额完全相同
                    transaction.getAmount().compareTo(amount) == 0 &&
                    // 描述完全相同
                    Objects.equals(transaction.getDescription(), description) &&
                    // 类型相同
                    transaction.getType() == type &&
                    // 在时间窗口内
                    !transaction.getTimestamp().isBefore(windowStart) &&
                    !transaction.getTimestamp().isAfter(now))
                .collect(Collectors.toList());
    }
} 