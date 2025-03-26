package com.example.banking.stress;

import com.example.banking.dto.CreateTransactionRequest;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.UpdateTransactionRequest;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("stress")
public class StressTestSuite {

    @LocalServerPort
    private int port;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    
    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/transactions";
        
        // 清空所有现有交易，确保测试环境干净
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        for (TransactionDTO transaction : transactions) {
            transactionService.deleteTransaction(transaction.getId());
        }
    }

    @Test
    public void testBulkTransactionCreation() {
        int numTransactions = 1000;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < numTransactions; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal(100 + Math.random() * 900),
                "Stress Test Transaction " + i,
                TransactionType.values()[i % TransactionType.values().length]
            );
            
            long startTime = System.currentTimeMillis();
            transactionService.createTransaction(request);
            long endTime = System.currentTimeMillis();
            
            responseTimes.add(endTime - startTime);
        }
        
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        System.out.println("批量创建 " + numTransactions + " 个交易:");
        System.out.println("平均响应时间: " + avgResponseTime + "ms");
        System.out.println("最大响应时间: " + Collections.max(responseTimes) + "ms");
        System.out.println("最小响应时间: " + Collections.min(responseTimes) + "ms");
        System.out.println("95th 百分位: " + calculatePercentile(responseTimes, 95) + "ms");
        
        // 验证平均处理时间在可接受范围内
        assertTrue(avgResponseTime < 10, "平均响应时间应小于10ms");
    }

    @Test
    public void testConcurrentTransactionCreation() throws InterruptedException {
        int numThreads = 100;
        int transactionsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<List<Long>>> futures = new ArrayList<>();
        
        // 创建并启动所有线程
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                List<Long> threadResponseTimes = new ArrayList<>();
                for (int j = 0; j < transactionsPerThread; j++) {
                    CreateTransactionRequest request = new CreateTransactionRequest(
                        new BigDecimal(200 + Math.random() * 800),
                        "Concurrent Transaction Thread-" + threadId + "-" + j,
                        TransactionType.values()[j % TransactionType.values().length]
                    );
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);
                    
                    long startTime = System.currentTimeMillis();
                    restTemplate.exchange(baseUrl, HttpMethod.POST, entity, TransactionDTO.class);
                    long endTime = System.currentTimeMillis();
                    
                    threadResponseTimes.add(endTime - startTime);
                }
                return threadResponseTimes;
            }));
        }
        
        // 收集所有响应时间
        List<Long> allResponseTimes = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            try {
                allResponseTimes.addAll(future.get());
            } catch (ExecutionException e) {
                fail("并发测试执行失败: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(terminated, "线程池未在预期时间内关闭");
        
        // 分析结果
        double avgResponseTime = allResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        System.out.println("并发测试 (" + numThreads + " 线程, 每线程 " + transactionsPerThread + " 交易):");
        System.out.println("总请求数: " + allResponseTimes.size());
        System.out.println("平均响应时间: " + avgResponseTime + "ms");
        System.out.println("最大响应时间: " + Collections.max(allResponseTimes) + "ms");
        System.out.println("最小响应时间: " + Collections.min(allResponseTimes) + "ms");
        System.out.println("95th 百分位: " + calculatePercentile(allResponseTimes, 95) + "ms");
        
        // 验证平均响应时间在可接受范围内
        assertTrue(avgResponseTime < 50, "在高并发下，平均响应时间应小于50ms");
    }

    @Test
    public void testConcurrentQueries() throws InterruptedException {
        // 准备测试数据
        int testDataSize = 500;
        for (int i = 0; i < testDataSize; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal(100 + i),
                "Test Data " + i,
                TransactionType.values()[i % TransactionType.values().length]
            );
            transactionService.createTransaction(request);
        }
        
        // 执行并发查询
        int numThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<List<Long>>> futures = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            final int page = i % 10;
            futures.add(executor.submit(() -> {
                List<Long> responseTimes = new ArrayList<>();
                
                // 执行不同类型的查询
                for (int j = 0; j < 5; j++) {
                    String url = baseUrl + "?page=" + page + "&size=20";
                    
                    long startTime = System.currentTimeMillis();
                    restTemplate.getForEntity(url, List.class);
                    long endTime = System.currentTimeMillis();
                    
                    responseTimes.add(endTime - startTime);
                    
                    // 增加一个随机的查询类型
                    if (j % 2 == 0) {
                        url = baseUrl + "?type=" + TransactionType.values()[j % TransactionType.values().length];
                    } else {
                        url = baseUrl + "?minAmount=100&maxAmount=500";
                    }
                    
                    startTime = System.currentTimeMillis();
                    restTemplate.getForEntity(url, List.class);
                    endTime = System.currentTimeMillis();
                    
                    responseTimes.add(endTime - startTime);
                }
                
                return responseTimes;
            }));
        }
        
        // 收集所有响应时间
        List<Long> allResponseTimes = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            try {
                allResponseTimes.addAll(future.get());
            } catch (ExecutionException e) {
                fail("并发查询测试执行失败: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(terminated, "线程池未在预期时间内关闭");
        
        // 分析结果
        double avgResponseTime = allResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        System.out.println("并发查询测试 (" + numThreads + " 线程):");
        System.out.println("总查询数: " + allResponseTimes.size());
        System.out.println("平均响应时间: " + avgResponseTime + "ms");
        System.out.println("最大响应时间: " + Collections.max(allResponseTimes) + "ms");
        System.out.println("最小响应时间: " + Collections.min(allResponseTimes) + "ms");
        System.out.println("95th 百分位: " + calculatePercentile(allResponseTimes, 95) + "ms");
        
        // 验证平均响应时间在可接受范围内
        assertTrue(avgResponseTime < 200, "在并发查询下，平均响应时间应小于200ms");
    }

    @Test
    public void testMixedWorkload() throws InterruptedException {
        // 准备初始测试数据
        int initialDataSize = 200;
        for (int i = 0; i < initialDataSize; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal(100 + i),
                "Initial Data " + i,
                TransactionType.values()[i % TransactionType.values().length]
            );
            transactionService.createTransaction(request);
        }
        
        // 执行混合工作负载
        int numThreads = 50;
        final AtomicInteger writeCounter = new AtomicInteger(0);
        final AtomicInteger readCounter = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, List<Long>>>> futures = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                Map<String, List<Long>> responseTimes = new HashMap<>();
                responseTimes.put("read", new ArrayList<>());
                responseTimes.put("write", new ArrayList<>());
                
                Random random = new Random();
                
                // 每个线程执行10个操作
                for (int j = 0; j < 10; j++) {
                    // 70%的几率执行读操作，30%的几率执行写操作
                    if (random.nextDouble() < 0.7) {
                        // 读操作
                        readCounter.incrementAndGet();
                        String url = baseUrl + "?page=" + (j % 5) + "&size=20";
                        if (j % 3 == 0) {
                            url = baseUrl + "?type=" + TransactionType.values()[j % TransactionType.values().length];
                        }
                        
                        long startTime = System.currentTimeMillis();
                        restTemplate.getForEntity(url, List.class);
                        long endTime = System.currentTimeMillis();
                        
                        responseTimes.get("read").add(endTime - startTime);
                    } else {
                        // 写操作
                        writeCounter.incrementAndGet();
                        if (random.nextBoolean()) {
                            // 创建新交易
                            CreateTransactionRequest request = new CreateTransactionRequest(
                                new BigDecimal(200 + random.nextDouble() * 800),
                                "Mixed Workload Write " + threadId + "-" + j,
                                TransactionType.values()[random.nextInt(TransactionType.values().length)]
                            );
                            
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);
                            
                            long startTime = System.currentTimeMillis();
                            restTemplate.exchange(baseUrl, HttpMethod.POST, entity, TransactionDTO.class);
                            long endTime = System.currentTimeMillis();
                            
                            responseTimes.get("write").add(endTime - startTime);
                        } else {
                            // 更新现有交易
                            // 首先获取一个随机ID (假设ID从1开始)
                            int randomId = random.nextInt(initialDataSize) + 1;
                            
                            UpdateTransactionRequest request = new UpdateTransactionRequest(
                                new BigDecimal(300 + random.nextDouble() * 700),
                                "Updated Mixed Workload " + threadId + "-" + j,
                                TransactionType.values()[random.nextInt(TransactionType.values().length)]
                            );
                            
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            HttpEntity<UpdateTransactionRequest> entity = new HttpEntity<>(request, headers);
                            
                            long startTime = System.currentTimeMillis();
                            try {
                                restTemplate.exchange(baseUrl + "/" + randomId, 
                                    HttpMethod.PUT, entity, TransactionDTO.class);
                            } catch (Exception e) {
                                // 忽略不存在的ID错误，这在混合负载测试中是正常的
                            }
                            long endTime = System.currentTimeMillis();
                            
                            responseTimes.get("write").add(endTime - startTime);
                        }
                    }
                }
                
                return responseTimes;
            }));
        }
        
        // 收集所有响应时间
        List<Long> readResponseTimes = new ArrayList<>();
        List<Long> writeResponseTimes = new ArrayList<>();
        
        for (Future<Map<String, List<Long>>> future : futures) {
            try {
                Map<String, List<Long>> result = future.get();
                readResponseTimes.addAll(result.get("read"));
                writeResponseTimes.addAll(result.get("write"));
            } catch (ExecutionException e) {
                fail("混合负载测试执行失败: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(terminated, "线程池未在预期时间内关闭");
        
        // 分析结果
        double avgReadResponseTime = readResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
                
        double avgWriteResponseTime = writeResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        System.out.println("混合负载测试 (" + numThreads + " 线程):");
        System.out.println("读操作数: " + readCounter.get());
        System.out.println("写操作数: " + writeCounter.get());
        System.out.println("读操作平均响应时间: " + avgReadResponseTime + "ms");
        System.out.println("写操作平均响应时间: " + avgWriteResponseTime + "ms");
        System.out.println("读操作95th百分位: " + calculatePercentile(readResponseTimes, 95) + "ms");
        System.out.println("写操作95th百分位: " + calculatePercentile(writeResponseTimes, 95) + "ms");
        
        // 验证平均响应时间在可接受范围内
        assertTrue(avgReadResponseTime < 50, "在混合负载下，读操作平均响应时间应小于50ms");
        assertTrue(avgWriteResponseTime < 200, "在混合负载下，写操作平均响应时间应小于200ms");
    }

    private long calculatePercentile(List<Long> responseTimes, double percentile) {
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        Collections.sort(sortedTimes);
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        return sortedTimes.get(Math.max(0, index));
    }
} 