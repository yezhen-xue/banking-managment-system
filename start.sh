#!/bin/bash

# 默认环境为dev
ENV=${1:-dev}

echo "正在启动银行交易管理系统，环境: $ENV"

# 使用Maven启动应用
./mvnw spring-boot:run -Dspring-boot.run.profiles=$ENV 