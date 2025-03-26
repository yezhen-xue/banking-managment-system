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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction testTransaction;
    private CreateTransactionRequest createRequest;
    private UpdateTransactionRequest updateRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置重复交易检测的时间窗口
        ReflectionTestUtils.setField(transactionService, "duplicateTimeWindow", 5);

        // 设置测试数据
        testTransaction = new Transaction(
                1L,
                new BigDecimal("100.50"),
                "测试交易",
                TransactionType.INCOME,
                LocalDateTime.now()
        );

        createRequest = new CreateTransactionRequest(
                new BigDecimal("100.50"),
                "测试交易",
                TransactionType.INCOME
        );

        updateRequest = new UpdateTransactionRequest(
                new BigDecimal("200.75"),
                "更新的测试交易",
                TransactionType.EXPENSE
        );
    }

    @Test
    void createTransaction() {
        // 设置模拟行为 - 没有重复交易
        when(transactionRepository.findPotentialDuplicates(
                any(BigDecimal.class), anyString(), any(TransactionType.class), anyInt()))
                .thenReturn(Collections.emptyList());
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // 执行测试
        TransactionDTO result = transactionService.createTransaction(createRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
        assertEquals(testTransaction.getAmount(), result.getAmount());
        assertEquals(testTransaction.getDescription(), result.getDescription());
        assertEquals(testTransaction.getType(), result.getType());

        // 验证交互
        verify(transactionRepository, times(1)).findPotentialDuplicates(
                createRequest.getAmount(), 
                createRequest.getDescription(), 
                createRequest.getType(), 
                5);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_duplicateDetected() {
        // 设置模拟行为 - 发现重复交易
        when(transactionRepository.findPotentialDuplicates(
                any(BigDecimal.class), anyString(), any(TransactionType.class), anyInt()))
                .thenReturn(Arrays.asList(testTransaction));
        
        // 验证异常被抛出
        DuplicateTransactionException exception = assertThrows(DuplicateTransactionException.class, () -> {
            transactionService.createTransaction(createRequest);
        });
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("存在重复交易"));
        assertTrue(exception.getMessage().contains(createRequest.getAmount().toString()));
        assertTrue(exception.getMessage().contains(createRequest.getDescription()));
        assertTrue(exception.getMessage().contains(createRequest.getType().toString()));
        
        // 验证交互
        verify(transactionRepository, times(1)).findPotentialDuplicates(
                createRequest.getAmount(), 
                createRequest.getDescription(), 
                createRequest.getType(), 
                5);
        // 确认save方法没有被调用
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_withNegativeAmount_shouldThrowException() {
        // 创建一个金额为负的请求
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                new BigDecimal("-10.00"),
                "无效交易",
                TransactionType.EXPENSE
        );
        
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(invalidRequest);
        });
        
        // 验证异常信息
        assertEquals("交易金额必须大于零", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getAllTransactions() {
        // 设置模拟行为
        when(transactionRepository.findAll()).thenReturn(Arrays.asList(testTransaction));

        // 执行测试
        List<TransactionDTO> results = transactionService.getAllTransactions();

        // 验证结果
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(testTransaction.getId(), results.get(0).getId());

        // 验证交互
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void getTransactionById_existing() {
        // 设置模拟行为
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // 执行测试
        TransactionDTO result = transactionService.getTransactionById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());

        // 验证交互
        verify(transactionRepository, times(1)).findById(1L);
    }

    @Test
    void getTransactionById_nonExisting() {
        // 设置模拟行为
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // 验证异常被抛出
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.getTransactionById(999L);
        });
        
        // 验证异常信息
        assertEquals("交易不存在，ID: 999", exception.getMessage());

        // 验证交互
        verify(transactionRepository, times(1)).findById(999L);
    }

    @Test
    void updateTransaction_existing() {
        // 设置模拟行为
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // 执行测试
        TransactionDTO result = transactionService.updateTransaction(1L, updateRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(updateRequest.getAmount(), result.getAmount());
        assertEquals(updateRequest.getDescription(), result.getDescription());
        assertEquals(updateRequest.getType(), result.getType());

        // 验证交互
        verify(transactionRepository, times(1)).findById(1L);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_nonExisting() {
        // 设置模拟行为
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // 验证异常被抛出
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.updateTransaction(999L, updateRequest);
        });
        
        // 验证异常信息
        assertEquals("交易不存在，ID: 999", exception.getMessage());

        // 验证交互
        verify(transactionRepository, times(1)).findById(999L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    
    @Test
    void updateTransaction_withNegativeAmount_shouldThrowException() {
        // 创建一个金额为负的请求
        UpdateTransactionRequest invalidRequest = new UpdateTransactionRequest(
                new BigDecimal("-10.00"),
                "无效交易更新",
                TransactionType.EXPENSE
        );
        
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.updateTransaction(1L, invalidRequest);
        });
        
        // 验证异常信息
        assertEquals("交易金额必须大于零", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_existing() {
        // 设置模拟行为
        when(transactionRepository.deleteById(1L)).thenReturn(true);

        // 执行测试 - 不应抛出异常
        assertDoesNotThrow(() -> {
            transactionService.deleteTransaction(1L);
        });

        // 验证交互
        verify(transactionRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTransaction_nonExisting() {
        // 设置模拟行为
        when(transactionRepository.deleteById(999L)).thenReturn(false);

        // 验证异常被抛出
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.deleteTransaction(999L);
        });
        
        // 验证异常信息
        assertEquals("交易不存在，ID: 999", exception.getMessage());

        // 验证交互
        verify(transactionRepository, times(1)).deleteById(999L);
    }

    @Test
    void getTransactionsByType() {
        // 设置模拟行为
        when(transactionRepository.findByType(TransactionType.INCOME))
                .thenReturn(Arrays.asList(testTransaction));

        // 执行测试
        List<TransactionDTO> results = transactionService.getTransactionsByType(TransactionType.INCOME);

        // 验证结果
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TransactionType.INCOME, results.get(0).getType());

        // 验证交互
        verify(transactionRepository, times(1)).findByType(TransactionType.INCOME);
    }
    
    @Test
    void getTransactionsByType_withNullType_shouldThrowException() {
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionsByType(null);
        });
        
        // 验证异常信息
        assertEquals("交易类型不能为空", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).findByType(any());
    }

    @Test
    void getTransactionsByAmountRange() {
        // 设置模拟行为
        when(transactionRepository.findByAmountRange(any(), any()))
                .thenReturn(Arrays.asList(testTransaction));

        // 执行测试
        List<TransactionDTO> results = transactionService.getTransactionsByAmountRange(
                new BigDecimal("50"), new BigDecimal("200"));

        // 验证结果
        assertNotNull(results);
        assertEquals(1, results.size());

        // 验证交互
        verify(transactionRepository, times(1)).findByAmountRange(
                new BigDecimal("50"), new BigDecimal("200"));
    }
    
    @Test
    void getTransactionsByAmountRange_withInvalidRange_shouldThrowException() {
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionsByAmountRange(
                    new BigDecimal("200"), new BigDecimal("100"));
        });
        
        // 验证异常信息
        assertEquals("最小金额不能大于最大金额", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).findByAmountRange(any(), any());
    }

    @Test
    void getTransactionsWithPagination() {
        // 设置模拟行为
        when(transactionRepository.findWithPagination(0, 10))
                .thenReturn(Arrays.asList(testTransaction));

        // 执行测试
        List<TransactionDTO> results = transactionService.getTransactionsWithPagination(0, 10);

        // 验证结果
        assertNotNull(results);
        assertEquals(1, results.size());

        // 验证交互
        verify(transactionRepository, times(1)).findWithPagination(0, 10);
    }
    
    @Test
    void getTransactionsWithPagination_withNegativePage_shouldThrowException() {
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionsWithPagination(-1, 10);
        });
        
        // 验证异常信息
        assertEquals("页码不能为负数", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).findWithPagination(anyInt(), anyInt());
    }
    
    @Test
    void getTransactionsWithPagination_withInvalidSize_shouldThrowException() {
        // 验证异常被抛出
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionsWithPagination(0, 0);
        });
        
        // 验证异常信息
        assertEquals("每页大小必须大于零", exception.getMessage());
        
        // 验证不与仓库交互
        verify(transactionRepository, never()).findWithPagination(anyInt(), anyInt());
    }

    @Test
    void getTransactionCount() {
        // 设置模拟行为
        when(transactionRepository.count()).thenReturn(5L);

        // 执行测试
        long result = transactionService.getTransactionCount();

        // 验证结果
        assertEquals(5L, result);

        // 验证交互
        verify(transactionRepository, times(1)).count();
    }
} 