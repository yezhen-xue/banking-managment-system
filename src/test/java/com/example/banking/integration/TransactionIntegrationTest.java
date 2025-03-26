package com.example.banking.integration;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @BeforeEach
    public void setup() {
        // 清理所有测试数据
        clearAllTransactions();
    }

    private void clearAllTransactions() {
        // 获取所有交易
        List<Long> allIds = transactionRepository.findAll().stream()
                .map(transaction -> transaction.getId())
                .toList();
                
        // 删除所有交易
        for (Long id : allIds) {
            transactionRepository.deleteById(id);
        }
    }

    @Test
    public void testTransactionLifecycle() throws Exception {
        // 1. 创建交易
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                new BigDecimal("100.50"),
                "集成测试交易",
                TransactionType.INCOME
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.description", is("集成测试交易")))
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andReturn();

        // 解析创建的交易
        String createResponseContent = createResult.getResponse().getContentAsString();
        TransactionDTO createdTransaction = objectMapper.readValue(createResponseContent, TransactionDTO.class);
        assertNotNull(createdTransaction.getId());

        Long transactionId = createdTransaction.getId();

        // 2. 获取特定交易
        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.intValue())))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.description", is("集成测试交易")));

        // 3. 更新交易
        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                new BigDecimal("200.75"),
                "更新后的集成测试交易",
                TransactionType.EXPENSE
        );

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.intValue())))
                .andExpect(jsonPath("$.amount", is(200.75)))
                .andExpect(jsonPath("$.description", is("更新后的集成测试交易")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        // 4. 获取所有交易
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 5. 获取交易总数
        MvcResult countResult = mockMvc.perform(get("/api/transactions/count"))
                .andExpect(status().isOk())
                .andReturn();

        String countResponseContent = countResult.getResponse().getContentAsString();
        Long count = Long.valueOf(countResponseContent);
        assertEquals(1L, count);

        // 6. 按类型过滤
        mockMvc.perform(get("/api/transactions?type=EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")));

        // 7. 按金额范围过滤
        mockMvc.perform(get("/api/transactions?minAmount=100&maxAmount=300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 8. 删除交易
        mockMvc.perform(delete("/api/transactions/{id}", transactionId))
                .andExpect(status().isNoContent());

        // 9. 确认交易已删除
        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isNotFound());

        // 10. 确认交易列表为空
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testValidationFailure() throws Exception {
        // 测试创建无效交易 (金额为负)
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                new BigDecimal("-100.50"), // 负金额
                "无效交易测试",
                TransactionType.EXPENSE
        );

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // 测试更新不存在的交易
        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                new BigDecimal("200.75"),
                "更新不存在的交易",
                TransactionType.EXPENSE
        );

        mockMvc.perform(put("/api/transactions/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPagination() throws Exception {
        // 创建10个交易
        for (int i = 0; i < 10; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.50").add(new BigDecimal(i)),
                    "分页测试交易 " + i,
                    i % 2 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE
            );

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // 测试第一页，每页5条
        mockMvc.perform(get("/api/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        // 测试第二页，每页5条
        mockMvc.perform(get("/api/transactions?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        // 测试过滤INCOME类型交易
        mockMvc.perform(get("/api/transactions?type=INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5))); // 应该有5个INCOME类型交易
        
        // 测试结束后，清理所有创建的交易
        clearAllTransactions();
    }
} 