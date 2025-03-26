package com.example.banking.performance;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TransactionPerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionService transactionService;

    @Test
    public void testConcurrentTransactionCreation() throws Exception {
        int numOfConcurrentRequests = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        // 测试开始时间
        long startTime = System.currentTimeMillis();
        
        // 创建并发请求
        for (int i = 0; i < numOfConcurrentRequests; i++) {
            int finalI = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                // 创建请求
                CreateTransactionRequest request = new CreateTransactionRequest(
                        new BigDecimal("100.50").add(new BigDecimal(finalI)),
                        "性能测试交易 " + finalI,
                        finalI % 3 == 0 ? TransactionType.INCOME : 
                        (finalI % 3 == 1 ? TransactionType.EXPENSE : TransactionType.TRANSFER)
                );
                
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);
                
                // 记录请求开始时间
                long requestStartTime = System.currentTimeMillis();
                
                // 发送请求
                ResponseEntity<?> response = restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/transactions",
                        entity,
                        Object.class
                );
                
                // 记录请求结束时间
                long requestEndTime = System.currentTimeMillis();
                
                // 返回请求耗时
                return requestEndTime - requestStartTime;
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有请求完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS); // 设置超时时间为30秒
        
        // 计算平均响应时间
        long totalResponseTime = 0;
        for (CompletableFuture<Long> future : futures) {
            totalResponseTime += future.get();
        }
        double averageResponseTime = (double) totalResponseTime / numOfConcurrentRequests;
        
        // 测试结束时间
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 输出性能测试结果
        System.out.println("====== 交易API性能测试结果 ======");
        System.out.println("并发请求数: " + numOfConcurrentRequests);
        System.out.println("总耗时: " + totalTime + " ms");
        System.out.println("平均响应时间: " + averageResponseTime + " ms");
        System.out.println("每秒处理请求数 (TPS): " + (numOfConcurrentRequests * 1000.0 / totalTime));
        
        // 断言平均响应时间不超过50毫秒
        assertTrue(averageResponseTime < 50, "平均响应时间应该小于50毫秒");
        
        // 关闭线程池
        executorService.shutdown();
    }

    @Test
    public void testPaginationPerformance() {
        // 首先创建一批交易数据
        for (int i = 0; i < 100; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("200.75").add(new BigDecimal(i)),
                    "分页性能测试交易 " + i,
                    i % 3 == 0 ? TransactionType.INCOME : 
                    (i % 3 == 1 ? TransactionType.EXPENSE : TransactionType.TRANSFER)
            );
            transactionService.createTransaction(request);
        }
        
        // 测试分页查询性能
        int numberOfQueries = 10;
        long totalResponseTime = 0;
        
        for (int i = 0; i < numberOfQueries; i++) {
            long startTime = System.currentTimeMillis();
            
            // 查询不同页码
            restTemplate.getForEntity(
                    "http://localhost:" + port + "/api/transactions?page=" + i + "&size=10",
                    Object.class
            );
            
            long endTime = System.currentTimeMillis();
            totalResponseTime += (endTime - startTime);
        }
        
        double averageResponseTime = (double) totalResponseTime / numberOfQueries;
        
        System.out.println("====== 分页查询性能测试结果 ======");
        System.out.println("查询次数: " + numberOfQueries);
        System.out.println("平均响应时间: " + averageResponseTime + " ms");
        
        // 断言平均响应时间不超过100毫秒
        assertTrue(averageResponseTime < 100, "分页查询平均响应时间应该小于100毫秒");
    }
} 