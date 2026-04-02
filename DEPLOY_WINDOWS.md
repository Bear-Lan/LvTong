# 绿通交通系统 - Windows 部署手册

## 目录

1. [环境准备](#1-环境准备)
2. [数据库初始化](#2-数据库初始化)
3. [后端部署](#3-后端部署)
4. [前端部署](#4-前端部署)
5. [启动顺序](#5-启动顺序)
6. [验证部署](#6-验证部署)
7. [常见问题](#7-常见问题)
8. [部署清单](#8-部署清单)

---

## 1. 环境准备

### 1.1 必需软件

| 软件 | 版本 | 下载地址 | 备注 |
|------|------|---------|------|
| MySQL | 8.0+ | https://dev.mysql.com/downloads/mysql/ | 安装时设置 root 密码为 `123456` |
| JDK | 18+ | https://adoptium.net/ | 推荐 Temurin 18 |
| Nginx | 最新版 | https://nginx.org/en/download.html | 下载 `nginx-x.x.x.zip`（Windows 版解压即用） |
| Node.js | 18+ | https://nodejs.org/ | 仅构建前端用，目标机不需要 |

> 项目未使用 Redis，无需安装。

### 1.2 环境变量

打开 **系统高级设置 → 环境变量**，新建或确认：

```
JAVA_HOME  →  D:\soft\jdk-18         （实际 JDK 安装路径）
Path       →  %JAVA_HOME%\bin        （追加到现有 Path）
```

验证是否生效（重新打开一个 cmd 窗口后执行）：

```cmd
java -version
```

### 1.3 创建目录

```cmd
mkdir D:\soft\FtpLvTong
mkdir D:\deploy\backend
mkdir D:\deploy\frontend
```

---

## 2. 数据库初始化

### 2.1 启动 MySQL

```cmd
net start mysql
```

### 2.2 创建数据库

```cmd
mysql -u root -p
```

```sql
CREATE DATABASE lvtong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### 2.3 导入数据

将 `lvtong.sql` 复制到目标机，执行：

```cmd
mysql -u root -p lvtong < D:\deploy\lvtong.sql
```

---

## 3. 后端部署

### 3.1 迁移 SSL 证书（开发机器上执行）

将原开发机器上的 `backend/src/main/resources/lvtong.p12` 复制到目标机 `D:\deploy\backend\lvtong.p12`。

> 证书必须和 jar 包放在同一目录下，jar 启动时会从 `file:lvtong.p12`（即 jar 包同目录）读取。

### 3.2 修改配置文件（在开发机上执行，打包前修改）

> `mvn package` 会将 `application.properties` 一起打包进 jar，所以配置必须在打包前改好。

编辑 `backend/src/main/resources/application.properties`，只需修改以下三项：

```properties
# 数据库密码（改为目标机MySQL的实际密码）
spring.datasource.password=123456

# SSL证书路径（指向jar同目录下的lvtong.p12）
server.ssl.key-store=file:lvtong.p12

# 文件上传路径（目标机实际路径）
upload.base-path=D:/soft/FtpLvTong
```

### 3.3 构建 jar 包（在开发机器上执行）

```cmd
cd d:\LvTongTransportDept\LvTongTransportDept\backend
mvnw.cmd clean package -DskipTests
```

> `mvnw.cmd` 是项目自带的 Maven Wrapper，Windows 下直接运行，无需额外安装 Maven。

生成的 jar 包：`backend\target\LvTongTransportDept-0.0.1-SNAPSHOT.jar`

### 3.4 迁移文件到目标机

将以下文件复制到目标机 `D:\deploy\backend\`：

```
LvTongTransportDept-0.0.1-SNAPSHOT.jar
lvtong.p12
```

> 项目已提供双击启动脚本 `启动后端.bat`，无需手动敲命令。

### 3.5 启动后端

**方式一：双击启动（推荐）**

双击 `启动后端.bat`，会自动在新窗口中启动后端服务。

> 窗口关闭 = 服务停止。看到 `Started LvTongTransportDeptApplication` 即启动成功。

**方式二：命令行启动**

```cmd
cd D:\deploy\backend
java -jar LvTongTransportDept-0.0.1-SNAPSHOT.jar
```

> 启动后观察控制台输出，确认无报错。看到 `Started LvTongTransportDeptApplication` 即启动成功。

确认后端正常运行，访问 `https://localhost:8090/api/mobile/health`，返回任意 JSON 表示正常。

---

## 4. 前端部署

### 4.1 构建前端（在开发机器上执行）

```cmd
cd d:\LvTongTransportDept\LvTongTransportDept\frontend
npm install
npm run build
```

生成的 `dist` 目录在 `frontend\dist`。

### 4.2 迁移文件到目标机

将 `dist` 目录复制到目标机 `D:\deploy\frontend\dist\`。

### 4.3 配置并启动 Nginx

**解压 Nginx**：下载 `nginx-x.x.x.zip`，解压到 `D:\nginx\`。

**配置**：将项目提供的 `nginx.conf`（见下方完整配置）整体替换 `D:\nginx\conf\nginx.conf`。

> 只需修改一处路径：`root D:/deploy/frontend/dist;` 改为实际 dist 所在路径。

完整 `nginx.conf`（Windows 版）：

```nginx
worker_processes  1;
events {
    worker_connections  1024;
}
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       80;
        server_name  localhost;

        location / {
            root   D:/deploy/frontend/dist;
            index  index.html index.htm;
            try_files $uri $uri/ /index.html;
        }

        location /api/ {
            proxy_pass https://localhost:8090/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;

            proxy_ssl_verify off;    # 后端使用自签名证书，关闭验证

            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        location /swagger-ui.html {
            proxy_pass https://localhost:8090/swagger-ui.html;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_ssl_verify off;
        }

        location /api-docs {
            proxy_pass https://localhost:8090/api-docs;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_ssl_verify off;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

**启动 Nginx**：

```cmd
cd D:\nginx
nginx.exe
```

> 启动后窗口无输出是正常的，访问 `http://localhost` 即可验证。

**常用命令**：

```cmd
nginx.exe -t        # 测试配置语法
nginx.exe -s reload # 重载配置
nginx.exe -s stop   # 停止
```

---

## 5. 启动顺序

按以下顺序启动：

| 顺序 | 服务 | 命令 | 等待确认 |
|------|------|------|---------|
| 1 | MySQL | `net start mysql` | cmd 无报错 |
| 2 | 后端 | `java -jar LvTongTransportDept-0.0.1-SNAPSHOT.jar` | 看到 `Started ...Application` |
| 3 | Nginx | `nginx.exe` | 访问 `http://localhost` 有响应 |

> 后端必须**先于 Nginx 启动**，否则 Nginx 代理会报错 `502 Bad Gateway`。

---

## 6. 验证部署

**通过浏览器访问（通过 Nginx）**：

| 服务 | 地址 | 预期结果 |
|------|------|---------|
| 前端首页 | http://localhost | 显示系统登录页面 |
| Swagger UI | http://localhost/swagger-ui.html | 显示 API 文档页面 |
| 后端接口文档 | http://localhost/api-docs | 返回 JSON 接口文档 |

**通过浏览器直接访问后端**（不经过 Nginx）：

| 服务 | 地址 | 预期结果 |
|------|------|---------|
| 后端健康检查 | https://localhost:8090/api/mobile/health | 返回任意 JSON 表示后端正常 |
| 后端接口文档 | https://localhost:8090/api-docs | 返回 JSON 接口文档 |

> 直接访问后端用 `https://`（自签名证书，浏览器会提示不安全，点「高级 → 继续前往」即可）。

---

## 7. 常见问题

### 7.1 后端启动失败：`java.io.FileNotFoundException: lvtong.p12`

原因：`lvtong.p12` 未放在 `D:\deploy\backend\lvtong.p12`。

解决：确认 `lvtong.p12` 与 jar 包在同一目录下，配置中写的是：

```properties
server.ssl.key-store=file:lvtong.p12
```

### 7.2 端口被占用

```cmd
netstat -ano | findstr :8090    # 查后端端口占用
netstat -ano | findstr :80      # 查 Nginx 端口占用
```

找到 PID 后，结束进程：

```cmd
taskkill /PID <PID> /F
```

### 7.3 MySQL 连接失败

- 确认 MySQL 已启动：`net start mysql`
- 确认密码正确：`application.properties` 中 `spring.datasource.password` 与实际一致
- 确认数据库已创建：登录 MySQL 执行 `SHOW DATABASES;`，应有 `lvtong`

### 7.4 Nginx 502 Bad Gateway

- 确认后端已启动（步骤 5 顺序 2）
- 确认后端端口是 `8090`，`nginx.conf` 中 `proxy_pass` 指向正确
- 用 `nginx.exe -t` 检查配置语法

### 7.5 浏览器访问 `http://localhost` 无响应

- 确认 Nginx 已启动
- 确认 80 端口未被其他程序占用（见 7.2）

---

## 8. 部署清单

```
环境安装
[ ] MySQL 8.0+ 已安装，root 密码为 123456
[ ] JDK 18+ 已安装，JAVA_HOME 已配置
[ ] Nginx 已解压，nginx.exe 可执行
[ ] D:\soft\FtpLvTong 目录已创建
[ ] D:\deploy\backend\ 目录已创建
[ ] D:\deploy\frontend\ 目录已创建

SSL 证书
[ ] lvtong.p12 已放置在 D:\deploy\backend\

数据库
[ ] MySQL 已启动
[ ] 数据库 lvtong 已创建
[ ] lvtong.sql 已成功导入

后端
[ ] jar 包已在 D:\deploy\backend\
[ ] 后端已启动，看到 Started ...Application

前端
[ ] dist 目录已在 D:\deploy\frontend\dist\

Nginx
[ ] nginx.conf 已替换（dist 路径正确）
[ ] Nginx 已启动
[ ] 访问 http://localhost 有响应

验证
[ ] 前端页面可正常打开
[ ] 后端接口可正常访问
```
