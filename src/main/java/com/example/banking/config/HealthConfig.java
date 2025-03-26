package com.example.banking.config;

import com.example.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

    private final TransactionRepository transactionRepository;

    @Autowired
    public HealthConfig(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Bean
    public HealthIndicator dataStoreHealthIndicator() {
        return () -> {
            try {
                // 尝试查询总数，检查数据存储是否正常工作
                long count = transactionRepository.count();
                return Health.up()
                        .withDetail("total_transactions", count)
                        .withDetail("status", "Data store is operational")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("status", "Data store is not operational")
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator applicationHealthIndicator() {
        return () -> {
            // 这里可以添加更多应用状态检查
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUtilization = 1.0 - ((double) freeMemory / totalMemory);

            Health.Builder builder = Health.up();
            
            // 如果内存使用率超过85%，则标记为警告
            if (memoryUtilization > 0.85) {
                builder = Health.status("WARNING");
            }
            
            return builder
                    .withDetail("memory_free_bytes", freeMemory)
                    .withDetail("memory_total_bytes", totalMemory)
                    .withDetail("memory_max_bytes", maxMemory)
                    .withDetail("memory_utilization", String.format("%.2f%%", memoryUtilization * 100))
                    .withDetail("available_processors", runtime.availableProcessors())
                    .build();
        };
    }
} 