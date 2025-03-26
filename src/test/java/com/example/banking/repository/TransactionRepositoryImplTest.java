package com.example.banking.repository;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionRepositoryImpl的单元测试
 * 全面测试Repository层的所有方法和分支逻辑
 */
public class TransactionRepositoryImplTest {

    private TransactionRepositoryImpl repository;
    private Transaction testTransaction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepositoryImpl();
        now = LocalDateTime.now();
        
        // 创建测试数据 - 确保ID已分配
        testTransaction = new Transaction(
                1L, // 明确设置ID，避免空ID
                new BigDecimal("100.50"),
                "测试交易",
                TransactionType.INCOME,
                now
        );
    }

    @Test
    void testSave_newTransaction() {
        Transaction saved = repository.save(testTransaction);
        
        // 验证交易已保存
        assertNotNull(saved.getId());
        assertEquals(testTransaction.getAmount(), saved.getAmount());
        assertEquals(testTransaction.getDescription(), saved.getDescription());
        assertEquals(testTransaction.getType(), saved.getType());
        assertEquals(testTransaction.getTimestamp(), saved.getTimestamp());
    }
    
    @Test
    void testSave_existingTransaction() {
        // 先保存交易
        Transaction saved = repository.save(testTransaction);
        Long id = saved.getId();
        
        // 修改并再次保存
        saved.setAmount(new BigDecimal("200.75"));
        saved.setDescription("更新的交易");
        saved.setType(TransactionType.EXPENSE);
        
        Transaction updated = repository.save(saved);
        
        // 验证更新成功且ID保持不变
        assertEquals(id, updated.getId());
        assertEquals(new BigDecimal("200.75"), updated.getAmount());
        assertEquals("更新的交易", updated.getDescription());
        assertEquals(TransactionType.EXPENSE, updated.getType());
    }
    
    @Test
    void testFindById_existing() {
        // 先保存交易
        repository.save(testTransaction);
        
        // 然后查找
        Optional<Transaction> found = repository.findById(testTransaction.getId());
        
        assertTrue(found.isPresent());
        assertEquals(testTransaction.getId(), found.get().getId());
    }
    
    @Test
    void testFindById_notExisting() {
        Optional<Transaction> found = repository.findById(999L);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindAll_empty() {
        List<Transaction> all = repository.findAll();
        
        assertTrue(all.isEmpty());
    }
    
    @Test
    void testFindAll_withData() {
        repository.save(testTransaction);
        repository.save(new Transaction(
                2L, // 确保ID不冲突
                new BigDecimal("200.75"),
                "另一个交易",
                TransactionType.EXPENSE,
                now.minusDays(1)
        ));
        
        List<Transaction> all = repository.findAll();
        
        assertEquals(2, all.size());
    }
    
    @Test
    void testDeleteById_existing() {
        // 先保存交易
        repository.save(testTransaction);
        
        boolean deleted = repository.deleteById(testTransaction.getId());
        
        assertTrue(deleted);
        assertEquals(0, repository.count());
    }
    
    @Test
    void testDeleteById_notExisting() {
        boolean deleted = repository.deleteById(999L);
        
        assertFalse(deleted);
    }
    
    @Test
    void testFindByType() {
        // 保存两种类型的交易
        repository.save(testTransaction); // INCOME
        repository.save(new Transaction(
                2L, // 确保ID不冲突
                new BigDecimal("200.75"),
                "支出交易",
                TransactionType.EXPENSE,
                now
        ));
        
        // 按类型INCOME查询
        List<Transaction> incomeTransactions = repository.findByType(TransactionType.INCOME);
        assertEquals(1, incomeTransactions.size());
        assertEquals(TransactionType.INCOME, incomeTransactions.get(0).getType());
        
        // 按类型EXPENSE查询
        List<Transaction> expenseTransactions = repository.findByType(TransactionType.EXPENSE);
        assertEquals(1, expenseTransactions.size());
        assertEquals(TransactionType.EXPENSE, expenseTransactions.get(0).getType());
        
        // 按类型TRANSFER查询（无结果）
        List<Transaction> transferTransactions = repository.findByType(TransactionType.TRANSFER);
        assertTrue(transferTransactions.isEmpty());
    }
    
    @Test
    void testFindByAmountRange_bothLimits() {
        setupMultipleTransactions();
        
        // 查询50-150范围内的交易
        List<Transaction> result = repository.findByAmountRange(
                new BigDecimal("50.00"), 
                new BigDecimal("150.00"));
        
        assertEquals(2, result.size());
    }
    
    @Test
    void testFindByAmountRange_onlyMinAmount() {
        setupMultipleTransactions();
        
        // 只设置最小金额
        List<Transaction> result = repository.findByAmountRange(
                new BigDecimal("150.00"), 
                null);
        
        assertEquals(2, result.size());
    }
    
    @Test
    void testFindByAmountRange_onlyMaxAmount() {
        setupMultipleTransactions();
        
        // 只设置最大金额
        List<Transaction> result = repository.findByAmountRange(
                null, 
                new BigDecimal("100.50"));
        
        assertEquals(2, result.size());
    }
    
    @Test
    void testFindByAmountRange_noLimits() {
        setupMultipleTransactions();
        
        // 不设置任何金额限制
        List<Transaction> result = repository.findByAmountRange(null, null);
        
        assertEquals(4, result.size());
    }
    
    @Test
    void testFindByDateRange_bothDates() {
        setupMultipleTransactions();
        
        // 查询昨天到今天范围内的交易
        List<Transaction> result = repository.findByDateRange(
                now.minusDays(1).truncatedTo(ChronoUnit.DAYS), 
                now);
        
        assertEquals(3, result.size());
    }
    
    @Test
    void testFindByDateRange_onlyStartDate() {
        setupMultipleTransactions();
        
        // 只设置开始日期
        List<Transaction> result = repository.findByDateRange(
                now.minusDays(1).truncatedTo(ChronoUnit.DAYS), 
                null);
        
        assertEquals(3, result.size());
    }
    
    @Test
    void testFindByDateRange_onlyEndDate() {
        setupMultipleTransactions();
        
        // 只设置结束日期
        List<Transaction> result = repository.findByDateRange(
                null, 
                now.minusDays(1));
        
        assertEquals(3, result.size());
    }
    
    @Test
    void testFindByDateRange_noDates() {
        setupMultipleTransactions();
        
        // 不设置任何日期限制
        List<Transaction> result = repository.findByDateRange(null, null);
        
        assertEquals(4, result.size());
    }
    
    @Test
    void testFindWithPagination() {
        setupMultipleTransactions();
        
        // 第一页，2条记录
        List<Transaction> firstPage = repository.findWithPagination(0, 2);
        assertEquals(2, firstPage.size());
        
        // 第二页，2条记录
        List<Transaction> secondPage = repository.findWithPagination(2, 2);
        assertEquals(2, secondPage.size());
        
        // 超出范围的页
        List<Transaction> emptyPage = repository.findWithPagination(10, 2);
        assertTrue(emptyPage.isEmpty());
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(testTransaction);
        assertEquals(1, repository.count());
        
        repository.save(new Transaction(2L, new BigDecimal("200"), "第二笔交易", TransactionType.EXPENSE, now));
        assertEquals(2, repository.count());
        
        repository.deleteById(testTransaction.getId());
        assertEquals(1, repository.count());
    }
    
    @Test
    void testFindPotentialDuplicates_hasDuplicates() {
        // 保存一个交易
        repository.save(testTransaction);
        
        // 查找潜在重复
        List<Transaction> duplicates = repository.findPotentialDuplicates(
                testTransaction.getAmount(),
                testTransaction.getDescription(),
                testTransaction.getType(),
                5 // 5分钟窗口
        );
        
        assertEquals(1, duplicates.size());
    }
    
    @Test
    void testFindPotentialDuplicates_noDuplicatesDifferentAmount() {
        // 保存一个交易
        repository.save(testTransaction);
        
        // 查找金额不同的潜在重复
        List<Transaction> duplicates = repository.findPotentialDuplicates(
                new BigDecimal("200.00"),
                testTransaction.getDescription(),
                testTransaction.getType(),
                5
        );
        
        assertTrue(duplicates.isEmpty());
    }
    
    @Test
    void testFindPotentialDuplicates_noDuplicatesDifferentDescription() {
        // 保存一个交易
        repository.save(testTransaction);
        
        // 查找描述不同的潜在重复
        List<Transaction> duplicates = repository.findPotentialDuplicates(
                testTransaction.getAmount(),
                "不同的描述",
                testTransaction.getType(),
                5
        );
        
        assertTrue(duplicates.isEmpty());
    }
    
    @Test
    void testFindPotentialDuplicates_noDuplicatesDifferentType() {
        // 保存一个交易
        repository.save(testTransaction);
        
        // 查找类型不同的潜在重复
        List<Transaction> duplicates = repository.findPotentialDuplicates(
                testTransaction.getAmount(),
                testTransaction.getDescription(),
                TransactionType.EXPENSE,
                5
        );
        
        assertTrue(duplicates.isEmpty());
    }
    
    @Test
    void testFindPotentialDuplicates_noDuplicatesOutsideTimeWindow() {
        // 保存一个6分钟前的交易
        Transaction oldTransaction = new Transaction(
                2L, // 确保ID不冲突
                testTransaction.getAmount(),
                testTransaction.getDescription(),
                testTransaction.getType(),
                LocalDateTime.now().minusMinutes(6)
        );
        repository.save(oldTransaction);
        
        // 查找潜在重复，时间窗口设为5分钟
        List<Transaction> duplicates = repository.findPotentialDuplicates(
                testTransaction.getAmount(),
                testTransaction.getDescription(),
                testTransaction.getType(),
                5
        );
        
        assertTrue(duplicates.isEmpty());
    }
    
    /**
     * 初始化多个测试交易
     */
    private void setupMultipleTransactions() {
        // 第一个交易 - 今天，收入，100.50
        repository.save(testTransaction);
        
        // 第二个交易 - 昨天，支出，50.25
        repository.save(new Transaction(
                2L, // 确保ID不冲突
                new BigDecimal("50.25"),
                "昨天的支出",
                TransactionType.EXPENSE,
                now.minusDays(1)
        ));
        
        // 第三个交易 - 昨天，收入，150.75
        repository.save(new Transaction(
                3L, // 确保ID不冲突
                new BigDecimal("150.75"),
                "昨天的收入",
                TransactionType.INCOME,
                now.minusDays(1)
        ));
        
        // 第四个交易 - 两天前，转账，200.00
        repository.save(new Transaction(
                4L, // 确保ID不冲突
                new BigDecimal("200.00"),
                "前天的转账",
                TransactionType.TRANSFER,
                now.minusDays(2)
        ));
    }
} 