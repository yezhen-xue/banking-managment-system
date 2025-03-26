package com.example.banking.controller;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "交易管理", description = "交易CRUD操作API")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "创建新交易", description = "创建一笔新的交易记录")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "交易创建成功", 
                content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "400", description = "无效的请求数据", content = @Content),
        @ApiResponse(responseCode = "409", description = "检测到重复交易", content = @Content)
    })
    public ResponseEntity<TransactionDTO> createTransaction(
            @Parameter(description = "交易创建请求数据", required = true) 
            @RequestBody CreateTransactionRequest request) {
        TransactionDTO createdTransaction = transactionService.createTransaction(request);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "获取交易列表", description = "获取所有交易，支持分页和过滤")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取交易列表", 
                content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
    })
    public ResponseEntity<List<TransactionDTO>> getAllTransactions(
            @Parameter(description = "页码，从0开始") 
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "每页大小") 
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "交易类型") 
            @RequestParam(value = "type", required = false) TransactionType type,
            @Parameter(description = "最小金额") 
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @Parameter(description = "最大金额") 
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @Parameter(description = "开始日期 (ISO格式: yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam(value = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束日期 (ISO格式: yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam(value = "endDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // 获取所有交易
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        
        // 按类型过滤
        if (type != null) {
            transactions = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
        }
        
        // 按金额范围过滤
        if (minAmount != null || maxAmount != null) {
            transactions = transactions.stream()
                .filter(t -> (minAmount == null || t.getAmount().compareTo(minAmount) >= 0) &&
                           (maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0))
                .collect(Collectors.toList());
        }
        
        // 按日期范围过滤
        if (startDate != null || endDate != null) {
            transactions = transactions.stream()
                .filter(t -> (startDate == null || t.getTimestamp().isEqual(startDate) || t.getTimestamp().isAfter(startDate)) &&
                           (endDate == null || t.getTimestamp().isEqual(endDate) || t.getTimestamp().isBefore(endDate)))
                .collect(Collectors.toList());
        }
        
        // 应用分页
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, transactions.size());
        
        if (fromIndex >= transactions.size()) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
        }
        
        List<TransactionDTO> pagedTransactions = transactions.subList(fromIndex, toIndex);
        return new ResponseEntity<>(pagedTransactions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取交易", description = "根据提供的ID获取特定交易")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取交易", 
                content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "404", description = "交易不存在", content = @Content)
    })
    public ResponseEntity<TransactionDTO> getTransactionById(
            @Parameter(description = "交易ID", required = true) 
            @PathVariable Long id) {
        TransactionDTO transaction = transactionService.getTransactionById(id);
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新交易", description = "根据提供的ID更新交易")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "交易更新成功", 
                content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "404", description = "交易不存在", content = @Content),
        @ApiResponse(responseCode = "400", description = "无效的请求数据", content = @Content)
    })
    public ResponseEntity<TransactionDTO> updateTransaction(
            @Parameter(description = "交易ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "交易更新请求数据", required = true) 
            @RequestBody UpdateTransactionRequest request) {
        TransactionDTO updatedTransaction = transactionService.updateTransaction(id, request);
        return new ResponseEntity<>(updatedTransaction, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除交易", description = "根据提供的ID删除交易")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "交易删除成功"),
        @ApiResponse(responseCode = "404", description = "交易不存在", content = @Content)
    })
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "交易ID", required = true) 
            @PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/count")
    @Operation(summary = "获取交易总数", description = "获取系统中交易记录的总数")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取交易总数", 
                content = @Content(schema = @Schema(implementation = Long.class)))
    })
    public ResponseEntity<Long> getTransactionCount() {
        long count = transactionService.getTransactionCount();
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
} 