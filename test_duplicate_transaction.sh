#!/bin/bash

# 银行交易管理系统 - 重复交易检测测试脚本
# 此脚本用于测试重复交易检测功能

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}银行交易管理系统 - 重复交易检测测试${NC}"
echo "========================================"
echo "此脚本将创建两个相同的交易，验证重复交易检测功能是否正常工作"
echo ""

# 检查应用是否在运行
echo -e "${YELLOW}检查应用是否在运行...${NC}"
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${RED}应用程序未运行。请先启动应用程序！${NC}"
    echo "使用以下命令启动应用："
    echo "  ./mvnw spring-boot:run"
    exit 1
fi
echo -e "${GREEN}应用程序正在运行${NC}"
echo ""

# 创建第一个交易
echo -e "${YELLOW}创建第一个交易...${NC}"
FIRST_RESPONSE=$(curl -s -X POST http://localhost:8080/api/transactions \
    -H "Content-Type: application/json" \
    -d '{"amount": 123.45, "description": "重复交易测试", "type": "EXPENSE"}')

echo "响应: $FIRST_RESPONSE"
if [[ "$FIRST_RESPONSE" == *"id"* ]]; then
    echo -e "${GREEN}第一个交易创建成功${NC}"
    TRANSACTION_ID=$(echo $FIRST_RESPONSE | grep -o '"id":[^,]*' | cut -d':' -f2)
    echo "交易ID: $TRANSACTION_ID"
else
    echo -e "${RED}第一个交易创建失败${NC}"
    exit 1
fi
echo ""

# 等待1秒
echo "等待1秒..."
sleep 1
echo ""

# 创建第二个相同交易（应该被检测为重复）
echo -e "${YELLOW}创建第二个相同交易（应该被检测为重复）...${NC}"
SECOND_RESPONSE=$(curl -s -X POST http://localhost:8080/api/transactions \
    -H "Content-Type: application/json" \
    -d '{"amount": 123.45, "description": "重复交易测试", "type": "EXPENSE"}')

echo "响应: $SECOND_RESPONSE"
if [[ "$SECOND_RESPONSE" == *"409"* ]] || [[ "$SECOND_RESPONSE" == *"Conflict"* ]] || [[ "$SECOND_RESPONSE" == *"重复交易"* ]]; then
    echo -e "${GREEN}重复交易检测成功！系统正确拒绝了重复交易${NC}"
else
    echo -e "${RED}重复交易检测失败！系统未能识别重复交易${NC}"
    echo "请检查TransactionServiceImpl中的重复交易检测逻辑"
fi
echo ""

# 清理测试数据
echo -e "${YELLOW}清理测试数据...${NC}"
DELETE_RESPONSE=$(curl -s -X DELETE http://localhost:8080/api/transactions/$TRANSACTION_ID -w "%{http_code}")
if [[ "$DELETE_RESPONSE" == "204" ]]; then
    echo -e "${GREEN}测试数据清理成功${NC}"
else
    echo -e "${RED}测试数据清理失败，响应码: $DELETE_RESPONSE${NC}"
fi
echo ""

echo -e "${YELLOW}测试完成${NC}"
echo "========================================" 