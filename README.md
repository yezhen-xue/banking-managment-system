# 银行交易管理系统

这是一个简单的银行交易管理系统，允许用户记录、查看和管理金融交易。

## 项目概述

本应用程序是一个基于Spring Boot的Java应用，提供RESTful API来管理银行交易。主要功能包括：

- 创建新交易
- 修改现有交易
- 删除交易
- 查询交易列表（支持分页和过滤）

## 技术栈

- Java 21
- Spring Boot 3.2.0
- Spring Web
- Spring Cache (使用Caffeine缓存实现)
- Spring Validation
- Spring Actuator
- SpringDoc OpenAPI (Swagger)
- Maven
- Docker/Kubernetes (用于容器化)

## 架构设计思路

本系统采用经典的分层架构，专注于高性能、线程安全和可扩展性。

### 分层设计

1. **表现层 (Controller)**：
   - 负责处理HTTP请求和响应
   - 实现RESTful API端点
   - 提供API文档（通过Swagger）
   - 处理请求参数验证和基本错误处理

2. **业务层 (Service)**：
   - 封装核心业务逻辑
   - 实现事务管理
   - 提供缓存机制
   - 协调不同组件间的交互
   - 处理更复杂的业务规则验证
   - 检测和防止重复交易提交

3. **数据访问层 (Repository)**：
   - 提供数据访问抽象
   - 实现内存数据存储
   - 支持各种查询需求（分页、过滤等）
   - 确保线程安全的数据访问
   - 支持查找潜在重复交易

4. **模型层 (Model/DTO)**：
   - 定义业务实体（Transaction）
   - 提供数据传输对象（DTO）
   - 清晰分离内部模型和API契约

### 关键技术决策

1. **内存数据存储**：
   - 使用ConcurrentHashMap实现线程安全的数据存储
   - 采用AtomicLong生成唯一ID，避免并发问题
   - 设计接口与实现分离，便于未来切换到其他存储方式

2. **缓存策略**：
   - 使用Caffeine高性能缓存库
   - 实现按需缓存失效，保证数据一致性
   - 对频繁请求接口（如获取所有交易）进行缓存优化

3. **并发处理**：
   - 无状态设计，提高系统可扩展性
   - 使用线程安全的数据结构和操作
   - 实现乐观并发控制，通过版本管理避免并发冲突

4. **性能优化**：
   - 实现分页查询减少内存压力
   - 提供多种过滤方式优化查询效率
   - 使用缓存减少计算开销

5. **错误处理**：
   - 集中式全局异常处理
   - 详细的错误消息和HTTP状态码
   - 提供业务异常（如重复交易、资源不存在等）的专门处理

6. **重复交易检测**：
   - 基于交易金额、描述和类型进行重复检测
   - 可配置的时间窗口（默认5分钟）内检测重复提交
   - 当检测到重复交易时返回409 Conflict状态码和详细错误信息

### 高可用设计

1. **无状态服务**：
   - 服务本身不保存状态信息
   - 可以水平扩展，部署多个实例

2. **健康检查**：
   - 提供监控端点检查系统状态
   - 支持Docker容器健康检查

3. **故障恢复**：
   - 设计容错机制，优雅处理异常情况
   - 实现重试策略处理暂时性故障

## API 端点

### 交易管理

| 方法   | URL                     | 描述                 | 请求体示例                                                    | 响应示例                                                     |
|------|-------------------------|---------------------|-----------------------------------------------------------|-----------------------------------------------------------|
| POST | /api/transactions       | 创建新交易             | `{"amount": 100.50, "description": "groceries", "type": "EXPENSE"}` | `{"id": 1, "amount": 100.50, "description": "groceries", "type": "EXPENSE", "timestamp": "2023-05-01T10:30:00"}` |
| GET  | /api/transactions       | 获取所有交易(支持分页和过滤) | N/A                                                         | `[{"id": 1, "amount": 100.50, "description": "groceries", "type": "EXPENSE", "timestamp": "2023-05-01T10:30:00"}]` |
| GET  | /api/transactions/{id}  | 获取特定交易            | N/A                                                         | `{"id": 1, "amount": 100.50, "description": "groceries", "type": "EXPENSE", "timestamp": "2023-05-01T10:30:00"}` |
| PUT  | /api/transactions/{id}  | 更新交易               | `{"amount": 120.75, "description": "weekly groceries", "type": "EXPENSE"}` | `{"id": 1, "amount": 120.75, "description": "weekly groceries", "type": "EXPENSE", "timestamp": "2023-05-01T10:30:00"}` |
| DELETE | /api/transactions/{id} | 删除交易              | N/A                                                         | `204 No Content`                                           |

### 异常处理

| 状态码 | 描述 | 触发条件 | 响应示例 |
|------|-----|--------|---------|
| 400 | Bad Request | 请求数据格式错误或验证失败 | `{"status": 400, "message": "交易金额必须大于零", "timestamp": "2023-05-01T10:30:00"}` |
| 404 | Not Found | 请求的交易不存在 | `{"status": 404, "message": "ID为5的交易不存在", "timestamp": "2023-05-01T10:30:00"}` |
| 409 | Conflict | 检测到重复交易 | `{"status": 409, "message": "存在重复交易: 金额=100.50, 描述=groceries, 类型=EXPENSE", "timestamp": "2023-05-01T10:30:00"}` |
| 500 | Internal Server Error | 服务器内部错误 | `{"status": 500, "message": "处理请求时发生内部错误", "timestamp": "2023-05-01T10:30:00"}` |

### 筛选和分页

支持以下查询参数：

- `page`: 页码，从0开始（默认: 0）
- `size`: 每页大小（默认: 20）
- `type`: 交易类型（INCOME, EXPENSE, TRANSFER）
- `minAmount`: 最小金额
- `maxAmount`: 最大金额
- `startDate`: 开始日期（ISO格式）
- `endDate`: 结束日期（ISO格式）

### API 文档 (Swagger)

应用程序集成了Swagger文档，您可以通过以下URL访问：

- Swagger UI: `/swagger-ui.html`
- OpenAPI 规范: `/api-docs`

这提供了一个交互式界面，允许您直接测试API端点。

### 监控端点

Spring Actuator提供以下端点：

- `/actuator/health`: 健康检查
- `/actuator/info`: 应用信息
- `/actuator/metrics`: 应用指标（开发环境）

## 构建和测试

系统采用全面的测试策略，确保代码质量和系统稳定性：

### 单元测试

- **服务层测试**：测试业务逻辑和服务方法
- **仓库层测试**：测试数据访问和查询方法
- **模型测试**：测试模型类的行为和属性

### 集成测试

- **API集成测试**：测试完整API流程，包括创建、读取、更新和删除操作
- **缓存集成测试**：测试缓存机制的正确性
- **服务间集成测试**：测试各服务组件的协调工作

### 压力测试

- **批量操作测试**：测试大批量交易创建和查询的性能
- **并发测试**：测试在高并发环境下的系统表现
- **混合负载测试**：模拟真实环境下的读写混合操作
- **资源利用率测试**：监测系统在负载下的CPU、内存和网络使用情况

### 代码覆盖率

系统采用JaCoCo工具监控代码覆盖率，当前覆盖率情况：

| 包名 | 指令覆盖率 | 分支覆盖率 | 方法覆盖率 | 类覆盖率 |
|------|-----------|------------|-----------|---------|
| DTO层 | 100% | 100% | 100% | 100% |
| Model层 | 92% | 87% | 85% | 100% |
| Service层 | 90% | 63% | 93% | 100% |
| Controller层 | 79% | 44% | 90% | 100% |
| Exception层 | 51% | 0% | 64% | 83% |
| Config层 | 52% | 0% | 78% | 100% |
| Repository层 | 100% | 94% | 100% | 100% |
| 整体覆盖率 | 79% | 66% | 86% | 94% |

#### 查看完整覆盖率报告

```bash
# 生成测试覆盖率报告
./mvnw clean test jacoco:report

# 报告位置
# target/site/jacoco/index.html
```

#### 覆盖率目标

项目设定的代码覆盖率目标为：
- 指令覆盖率：≥75%
- 分支覆盖率：≥60%
- 方法覆盖率：≥80%
- 类覆盖率：≥85%

## 性能优化

系统通过以下方式实现高性能：

1. **缓存机制**：使用Caffeine缓存提高查询性能
   - 交易列表缓存
   - 单个交易缓存
   - 按需清除缓存

2. **高效数据结构**：
   - 使用ConcurrentHashMap保证线程安全
   - 使用AtomicLong生成唯一ID

3. **高效查询**：
   - 支持分页查询减少内存使用
   - 支持多种过滤条件优化查询效率

## 错误处理

系统实现了全面的错误处理机制：
- 400 Bad Request: 请求数据格式错误或验证失败
- 404 Not Found: 请求的资源不存在
- 409 Conflict: 检测到重复交易提交（相同金额、描述和类型的交易在5分钟内重复提交）
- 500 Internal Server Error: 服务器内部错误

## 容器化

系统支持Docker容器化部署：

- 使用多阶段构建减小镜像大小
- 提供docker-compose.yml文件简化部署
- 内置健康检查确保服务可用性

## 如何运行

### 使用Maven运行

```bash
cd banking
./mvnw spring-boot:run
```

### 使用Docker运行

```bash
cd banking
docker build -t banking-app .
docker run -p 8080:8080 banking-app
```

### 使用Docker Compose运行

```bash
cd banking
docker-compose up -d
```

## 如何测试

### 单元测试

```bash
./mvnw test
```

### 集成测试

```bash
./mvnw test -Dtest=*IntegrationTest
```

### 性能测试

```bash
./mvnw test -Dtest=*StressTest*
```

### 重复交易检测测试

项目提供了一个简单的测试脚本，用于验证重复交易检测功能：

在Linux/Mac上：
```bash
# 添加执行权限
chmod +x test_duplicate_transaction.sh
# 运行测试脚本
./test_duplicate_transaction.sh
```

在Windows上，可以使用Powershell执行：
```powershell
# 确保应用程序正在运行
# 创建第一个交易
$firstResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" -Method Post -ContentType "application/json" -Body '{"amount": 123.45, "description": "重复交易测试", "type": "EXPENSE"}'
Write-Host "第一个交易创建成功，ID: $($firstResponse.id)"

# 尝试创建相同的交易（应该被检测为重复）
try {
    $secondResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" -Method Post -ContentType "application/json" -Body '{"amount": 123.45, "description": "重复交易测试", "type": "EXPENSE"}'
    Write-Host "测试失败：重复交易未被检测" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 409) {
        Write-Host "测试成功：重复交易被正确检测并拒绝" -ForegroundColor Green
    } else {
        Write-Host "测试失败：错误代码 $statusCode" -ForegroundColor Red
    }
}

# 清理测试数据
Invoke-RestMethod -Uri "http://localhost:8080/api/transactions/$($firstResponse.id)" -Method Delete
Write-Host "测试数据已清理"
```

### API测试

可以使用curl、Postman或浏览器访问：

```bash
curl -X GET http://localhost:8080/api/transactions
```

或者访问Web界面：http://localhost:8080/

或使用Swagger UI进行测试：http://localhost:8080/swagger-ui.html

## 环境配置

项目支持多环境配置，通过独立的配置文件实现：

- **开发环境 (dev)**：适用于开发，启用日志和监控
  - 配置文件：`application-dev.properties`
  - 中等级别缓存配置
  - 完整的监控端点
  
- **测试环境 (test)**：禁用缓存，启用详细日志
  - 配置文件：`application-test.properties`
  - 禁用缓存提高测试一致性
  - DEBUG级别日志
  - 完整监控端点用于测试验证
  
- **生产环境 (prod)**：启用高级缓存，限制日志级别，增强安全性
  - 配置文件：`application-prod.properties`
  - 高性能缓存配置
  - 最小化日志（仅WARN级别）
  - 限制监控端点，增强安全性

### 启动不同环境的应用

#### 使用脚本启动

**Linux/Mac:**
```bash
# 启动开发环境（默认）
./start.sh

# 启动测试环境
./start.sh test

# 启动生产环境
./start.sh prod
```

**Windows:**
```batch
# 启动开发环境（默认）
start.bat

# 启动测试环境
start.bat test

# 启动生产环境
start.bat prod
```

#### 使用Maven直接启动

```bash
# 启动开发环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 启动测试环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 启动生产环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### 使用Docker启动

```bash
# 启动开发环境
docker run -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=dev" banking-app

# 启动测试环境
docker run -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=test" banking-app

# 启动生产环境
docker run -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=prod" banking-app
```

## 未来工作计划

基于当前测试覆盖率分析，我们计划在下一阶段优先改进以下方面：

### 测试覆盖率提升

1. **异常处理测试完善（优先级最高）**：
   - 异常层覆盖率仅为51%，分支覆盖率为0%，需要显著改进
   - 增加对`GlobalExceptionHandler`的单元测试，特别是针对各种异常情况
   - 为所有异常类实现专门的测试用例，特别是`ValidationErrorResponse`类
   - 重点测试`DuplicateTransactionException`的处理流程

2. **配置类测试加强（优先级高）**：
   - 配置类分支覆盖率为0%，指令覆盖率仅为52%
   - 增加对健康检查配置的单元测试，特别是lambda方法的测试
   - 完善Swagger配置测试
   - 测试应用配置在不同环境下的行为

3. **控制器层测试改进**：
   - 控制器层分支覆盖率仅为44%，需要完善
   - 增加日期范围过滤测试，特别是`lambda$getAllTransactions$2`方法
   - 测试更多边缘情况和无效输入处理

4. **服务层分支覆盖改进**：
   - 虽然服务层整体覆盖率良好，但分支覆盖率仅为63%
   - 完善`getTransactionsByDateRange`方法的测试，该方法当前未被测试

### 功能增强计划

1. **数据持久化**：
   - 引入数据库存储替代内存存储
   - 支持事务管理
   - 实现数据备份和恢复机制

2. **API鲁棒性提升**：
   - 完善错误处理机制
   - 增强参数验证逻辑
   - 实现更友好的错误消息

3. **用户认证与授权**：
   - 添加基于Spring Security的身份验证
   - 实现基于角色的访问控制
   - 支持JWT或OAuth2授权

4. **性能优化**：
   - 优化查询性能
   - 实现更智能的缓存策略
   - 添加性能监控指标

## 技术债务处理计划

为确保项目长期健康发展，我们计划逐步处理以下技术债务：

1. **测试改进**:
   - 提高异常处理层测试覆盖率（目标从51%提升至80%+）
   - 改进控制器层分支覆盖率（目标从44%提升至70%+）
   - 完善配置类测试（特别是健康检查配置）
   - 添加端到端测试和契约测试
   - 实现持续集成测试自动化

2. **文档完善**:
   - 持续更新API文档
   - 添加架构决策记录(ADR)
   - 完善开发者指南和运维手册

3. **代码质量**:
   - 实施更严格的代码审查流程
   - 添加静态代码分析工具
   - 定期重构和优化代码结构

## 项目进度

- [x] 基础框架搭建
- [x] 核心API实现
- [x] 缓存机制集成
- [x] 前端页面实现
- [x] 单元测试编写
- [x] 集成测试编写
- [x] 性能测试编写
- [x] 仓库层测试覆盖率优化（从36%提升至100%）
- [x] 重复交易检测功能实现
- [x] Docker容器化支持
- [x] Docker Compose配置
- [x] 监控端点配置
- [x] API文档集成 (Swagger)
- [ ] 异常处理测试完善
- [ ] 控制器层分支覆盖率改进
- [ ] 配置类测试加强
- [ ] 数据持久化实现

## 外部依赖

- Spring Boot 框架
- Caffeine 缓存库
- SpringDoc OpenAPI (Swagger)

## 维护与支持

该项目作为示例应用程序提供，可用于学习和演示目的。 

## 业务规则

系统实现了以下业务规则：

1. **数据验证**：
   - 交易金额必须大于零
   - 交易描述不能为空且长度在1-100字符之间
   - 交易类型必须是有效的枚举值（INCOME、EXPENSE、TRANSFER）

2. **重复交易检测**：
   - 系统会检测在配置的时间窗口内（默认5分钟）有相同金额、描述和类型的交易
   - 当检测到重复交易时，系统会拒绝创建新交易并返回409 Conflict状态码
   - 此机制有助于防止因网络问题导致的意外重复提交 