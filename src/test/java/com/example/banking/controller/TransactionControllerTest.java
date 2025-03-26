package com.example.banking.controller;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.exception.BusinessException;
import com.example.banking.exception.TransactionNotFoundException;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionDTO testTransactionDTO;
    private CreateTransactionRequest createRequest;
    private UpdateTransactionRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testTransactionDTO = new TransactionDTO(
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
    void createTransaction() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(testTransactionDTO);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.description", is("测试交易")))
                .andExpect(jsonPath("$.type", is("INCOME")));
    }
    
    @Test
    void createTransaction_invalidAmount() throws Exception {
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                new BigDecimal("-50.00"),
                "无效交易",
                TransactionType.EXPENSE
        );
        
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new BusinessException("交易金额必须大于零", HttpStatus.BAD_REQUEST.value(), "Invalid Amount"));

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTransactions() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(Arrays.asList(testTransactionDTO));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100.50)))
                .andExpect(jsonPath("$[0].description", is("测试交易")))
                .andExpect(jsonPath("$[0].type", is("INCOME")));
    }

    @Test
    void getAllTransactions_withFilters_byType() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(Arrays.asList(testTransactionDTO));

        mockMvc.perform(get("/api/transactions?type=INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("INCOME")));
    }

    @Test
    void getAllTransactions_withFilters_byAmountRange() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(Arrays.asList(testTransactionDTO));

        mockMvc.perform(get("/api/transactions?minAmount=50.00&maxAmount=150.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAllTransactions_empty() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTransactionById_existing() throws Exception {
        when(transactionService.getTransactionById(1L))
                .thenReturn(testTransactionDTO);

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.description", is("测试交易")))
                .andExpect(jsonPath("$.type", is("INCOME")));
    }

    @Test
    void getTransactionById_nonExisting() throws Exception {
        when(transactionService.getTransactionById(999L))
                .thenThrow(new TransactionNotFoundException(999L));

        mockMvc.perform(get("/api/transactions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransaction_existing() throws Exception {
        TransactionDTO updatedTransaction = new TransactionDTO(
                1L,
                updateRequest.getAmount(),
                updateRequest.getDescription(),
                updateRequest.getType(),
                LocalDateTime.now()
        );
        
        when(transactionService.updateTransaction(eq(1L), any(UpdateTransactionRequest.class)))
                .thenReturn(updatedTransaction);

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(200.75)))
                .andExpect(jsonPath("$.description", is("更新的测试交易")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));
    }

    @Test
    void updateTransaction_nonExisting() throws Exception {
        when(transactionService.updateTransaction(eq(999L), any(UpdateTransactionRequest.class)))
                .thenThrow(new TransactionNotFoundException(999L));

        mockMvc.perform(put("/api/transactions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void updateTransaction_invalidData() throws Exception {
        UpdateTransactionRequest invalidRequest = new UpdateTransactionRequest(
                new BigDecimal("-50.00"),
                "无效更新",
                TransactionType.EXPENSE
        );
        
        when(transactionService.updateTransaction(eq(1L), any(UpdateTransactionRequest.class)))
                .thenThrow(new BusinessException("交易金额必须大于零", HttpStatus.BAD_REQUEST.value(), "Invalid Amount"));

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTransaction_existing() throws Exception {
        // 不需要模拟返回值，因为方法已经改为void
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isNoContent());
                
        verify(transactionService, times(1)).deleteTransaction(1L);
    }

    @Test
    void deleteTransaction_nonExisting() throws Exception {
        doThrow(new TransactionNotFoundException(999L))
                .when(transactionService).deleteTransaction(999L);

        mockMvc.perform(delete("/api/transactions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionCount() throws Exception {
        when(transactionService.getTransactionCount()).thenReturn(5L);

        mockMvc.perform(get("/api/transactions/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(5)));
    }
    
    @Test
    void getTransactions_invalidPaginationParams() throws Exception {
        mockMvc.perform(get("/api/transactions?page=-1&size=10"))
                .andExpect(status().is5xxServerError());
    }
    
    @Test
    void getTransactions_invalidAmountRange() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions?minAmount=200&maxAmount=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
} 