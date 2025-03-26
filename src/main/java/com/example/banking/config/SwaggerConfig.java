package com.example.banking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("银行交易管理系统 API")
                        .description("用于管理银行交易的RESTful API")
                        .version("v1.0.0")
                )
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8080").description("本地开发环境")
                ));
    }
} 