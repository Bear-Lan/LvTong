@echo off
chcp 65001 >nul
title 绿通交通系统 - 后端服务

cd /d "%~dp0"

echo ========================================
echo   绿通交通系统 - 后端服务启动
echo ========================================
echo.
echo 正在启动，请稍候...
echo 看到 "Started LvTongTransportDeptApplication" 即表示启动成功
echo 按任意键退出将停止服务
echo.
echo ========================================
echo.

java -jar LvTongTransportDept-0.0.1-SNAPSHOT.jar

pause
