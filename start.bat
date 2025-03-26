@echo off

REM 默认环境为dev
set ENV=%1
if "%ENV%"=="" set ENV=dev

echo 正在启动银行交易管理系统，环境: %ENV%

REM 使用Maven启动应用
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=%ENV% 