# 绿通运输查验系统 - 后端

基于 Spring Boot 4.0 的 RESTful API 服务，提供用户认证、车辆查验记录管理、图片上传及交通局上报等功能。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 4.0.3 | 核心框架 |
| Java | JDK 18 | 运行环境 |
| ORM | MyBatis Plus 3.5.16 | 数据库持久层 |
| 数据库 | MySQL 8.0 | 数据存储 |
| 认证 | JWT (jjwt 0.12.6) | Token 无状态认证 |
| 密码 | BCrypt | 单向哈希加密 |
| 文档 | springdoc-openapi 2.5.0 | Swagger UI 在线文档 |
| 文件上传 | Spring Servlet Multipart | 支持图片和 JSON 数据上传 |

## 项目结构

```
src/main/java/com/lvtong/LvTongTransportDept/
├── LvTongTransportDeptApplication.java      # 应用启动入口
│
├── config/                                    # 配置层
│   ├── MybatisPlusConfig.java                # MyBatis Plus 配置
│   ├── SwaggerConfig.java                    # Swagger UI + OpenAPI 3.0 配置
│   ├── SecurityConfig.java                   # 安全配置（跨域等）
│   ├── TransportDeptProperties.java          # 交通局上报配置
│   └── WebConfig.java                        # 拦截器注册、跨域配置
│
├── constant/                                    # 业务常量
│   ├── GroupConstants.java                    # 班组常量
│   ├── UserConstants.java                     # 用户状态常量
│   └── VehicleConstants.java                  # 车辆查验码值映射（核心）
│
├── controller/                                 # 接口层（RESTful）
│   ├── UserController.java                   # 用户管理 /api/user
│   ├── UserGroupController.java              # 用户组管理 /api/groups
│   ├── VehicleInspectionController.java       # 车辆查验 /api/inspection
│   ├── ImageController.java                   # 图片访问 /api/images
│   ├── ImageUploadController.java            # 图片上传 /api/upload
│   ├── TransportDeptUploadController.java    # 交通局上报 /api/transport-dept
│   └── MobileUploadController.java            # 手机端上传 /api/mobile
│
├── dto/                                        # 数据传输对象
│   ├── ApiResponse.java                      # 统一响应封装 { code, message, data }
│   ├── LoginRequest.java                     # 登录请求参数
│   └── UpdateUserRequest.java                # 更新用户参数
│
├── entity/                                     # 数据库实体类
│   ├── UserEntity.java                       # 用户表（users）
│   ├── UserGroup.java                        # 用户组表（user_groups）
│   ├── VehicleInspection.java                 # 查验记录表（vehicle_inspections）
│   └── AgriculturalProduct.java              # 农产品品种表（agricultural_products）
│
├── mapper/                                     # MyBatis Mapper 接口
│   ├── UserMapper.java                       # 用户 Mapper
│   ├── UserGroupMapper.java                  # 用户组 Mapper
│   ├── VehicleInspectionMapper.java           # 查验 Mapper
│   └── AgriculturalProductMapper.java         # 农产品 Mapper
│
├── service/                                    # 业务逻辑层
│   ├── UserService.java                      # 用户业务逻辑
│   ├── UserGroupService.java                 # 用户组业务逻辑
│   ├── VehicleInspectionService.java         # 查验业务逻辑（含多条件查询）
│   └── TransportDeptUploadService.java       # 交通局上报逻辑
│
├── interceptor/                               # 拦截器
│   └── JwtInterceptor.java                   # JWT Token 认证拦截器（排除登录/注册）
│
├── handler/                                    # 自动填充处理器
│   └── AutoFillHandler.java                  # 自动填充 createdTime / updatedTime
│
├── converter/                                 # 对象转换
│   └── TransportDeptConverter.java           # 查验记录转交通局上报格式
│
├── exception/                                 # 异常处理
│   ├── BusinessException.java               # 业务异常
│   └── GlobalExceptionHandler.java          # 全局异常处理
│
└── utils/                                     # 工具类
    ├── JwtUtil.java                         # JWT Token 签发与解析
    └── UploadUtils/                         # 上传工具
```

## 核心设计说明

### 统一响应格式

所有接口统一返回 `ApiResponse<T>` 结构：

```json
{
  "code": 200,           // 200=成功，其他=业务错误
  "message": "操作成功",  // 成功时可选，失败时携带错误信息
  "data": { ... }        // 成功时返回数据
}
```

### 部分更新语义

更新接口（PUT）采用**非空字段才更新**策略：

- 前端提交 `null` 字段 → **跳过，不更新**（保留数据库原值）
- 前端提交 `""` 空字符串 → **覆盖为空**
- 前端不提交的字段 → 等同于传 `null`

### 多条件组合查询

`VehicleInspectionService.searchWithConditions()` 使用 MyBatis Plus 的 **Wrapper** 实现动态 AND 组合查询：

```
车牌 LIKE '%xxx%'  AND  司机电话 = '138xxxx'  AND  操作员 = '张三'  AND  查验时间 BETWEEN ...
```

所有条件均为**可选项**，不传则不参与筛选。

### 码值到文本的自动转换

Controller 层在返回数据前，统一将码值转为可读文本：

| 数据库字段 | 返回字段 | 示例 |
|-----------|---------|------|
| `result_status` (1) | `resultStatusText` | "合格" |
| `nopass_type` (11) | `nopassTypeText` | "车货总质量超限" |
| `vehicle_type` ("11") | `vehicleClassText` | "一型货车" |
| `passcode_fee` ("125000") | 展示时÷100 | "1250.00 元" |

### 逻辑删除

所有实体支持逻辑删除，删除操作不会物理移除数据：

- `status = 1` → 已删除
- `status = 0` → 正常

## 已完成功能

### 用户管理 `/api/user`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户注册 | POST | /api/user/register | 用户名唯一性校验，BCrypt 密码加密 |
| 用户登录 | POST | /api/user/login | 支持图形验证码（Redis TTL 5分钟） |
| JWT 认证 | - | 全局 | Token 签发、请求拦截验证 |
| 用户列表 | GET | /api/user/list | 分页+排序（按 lastLoginTime DESC） |
| 用户详情 | GET | /api/user/{id} | 获取用户信息 |
| 更新用户 | PUT | /api/user/{id} | 支持部分字段更新 |
| 删除用户 | DELETE | /api/user/{id} | 逻辑删除 |
| 启用/禁用 | PUT | /api/user/{id}/status | 操作员权限管理 |
| 重置密码 | POST | /api/user/{id}/reset-password | 重置为默认密码 123456 |
| 用户手机号列表 | GET | /api/user/phones | 下拉选项 |
| 图形验证码 | GET | /api/user/captcha | 返回 PNG 图形验证码 |

### 用户组管理 `/api/groups`

| 功能 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | /api/groups |
| 详情 | GET | /api/groups/{id} |
| 新增 | POST | /api/groups |

### 车辆查验记录 `/api/inspection`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查验列表 | GET | /api/inspection/list | 分页+多条件筛选，按查验时间倒序 |
| 查验详情 | GET | /api/inspection/{id} | 返回所有字段含转换后文本 |
| 新增记录 | POST | /api/inspection | 全字段写入，自动维护时间 |
| 更新记录 | PUT | /api/inspection/{id} | **仅更新非空字段**，部分更新语义 |
| 删除记录 | DELETE | /api/inspection/{id} | 逻辑删除 |
| 统计看板 | GET | /api/inspection/dashboard | 当日/累计查验统计 |
| 农产品列表 | GET | /api/inspection/products | 农产品品种对照（绿通目录） |

**列表查询参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| plateNumber | String | 车牌号（模糊查询） |
| driverPhone | String | 司机电话（精确查询） |
| operatorName | String | 操作员（精确查询） |
| resultStatus | Integer | 查验结果（1=合格, 2=不合格） |
| startTime | String | 开始时间（yyyy-MM-dd） |
| endTime | String | 结束时间（yyyy-MM-dd） |
| page | Integer | 页码（从1开始，默认1） |
| pageSize | Integer | 每页条数（默认10） |

### 图片访问 `/api/images`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 读取图片 | GET | /api/images/{id} | 根据图片ID读取文件流返回 |
| 预览图片 | GET | /api/images/preview | 根据绝对路径返回文件流 |

### 图片上传 `/api/upload`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传图片 | POST | /api/upload/image | 接受 multipart/form-data，返回存储路径 |

### 手机端上传 `/api/mobile`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 图片上传 | POST | /api/mobile/upload/image | 手机端图片上传 |
| JSON数据上报 | POST | /api/mobile/upload/json | 查验记录上报 |
| 健康检查 | GET | /api/mobile/health | 服务健康检查 |

### 交通局上报 `/api/transport-dept`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 单条上报 | POST | /api/transport-dept/upload/{id} | 将查验记录上报至交通局系统 |
| 批量上报 | POST | /api/transport-dept/upload/batch | 批量上报 |

## 数据字典

### 查验结果（result_status）

| 值 | 含义 |
|----|------|
| 0 | 待查验 |
| 1 | 合格 |
| 2 | 不合格 |

### 不合格类型（nopass_type）

**绿通车辆（11-26）：**

| 值 | 含义 |
|----|------|
| 11 | 车货总质量超限 |
| 12 | 外廓尺寸超限 |
| 13 | 货物非《目录》内 |
| 14 | 货物属深加工产品 |
| 15 | 货物冷冻发硬、腐烂、变质 |
| 18 | 未达核定载质量和车厢容积80%以上 |
| 19 | 混装非鲜活农产品 |
| 20 | 混装《目录》外鲜活农产品超20% |
| 21 | 假冒绿通 |
| 22 | 未提供行驶证原件 |
| 23 | 提供的电子证件无法核定载质量 |
| 24 | 行驶证过期 |

**联合收割机（31-42）：**

| 值 | 含义 |
|----|------|
| 31 | 《作业证》无效 |
| 33 | 车货总质量超限 |
| 34 | 外廓尺寸超限 |
| 35 | 收割机未悬挂正式号牌 |
| 38 | 混装其他物品 |
| 39 | 无《作业证》 |

### 车型（vehicle_type）

| 值 | 含义 |
|----|------|
| 11 | 一型货车 |
| 12 | 二型货车 |
| 13 | 三型货车 |
| 14 | 四型货车 |
| 15 | 五型货车 |
| 16 | 六型货车 |

## 配置说明

`src/main/resources/application.properties` 主要配置项：

```properties
server.port=8090
server.ssl.enabled=true
server.ssl.key-store=classpath:lvtong.p12
server.ssl.key-store-password=LvTong2024
server.ssl.key-alias=lvtong
server.ssl.key-store-type=PKCS12

spring.datasource.url=jdbc:mysql://localhost:3306/lvtong?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456

spring.data.redis.host=localhost
spring.data.redis.port=6379

upload.base-path=D:/soft/FtpLvTong

transport-dept.https-url=https://testquickcheck.txffp.com
transport-dept.client-id=101003
transport-dept.client-key=M3eGF3kaC2
```

## 运行项目

### 环境要求

- JDK 18+
- MySQL 8.0+
- Maven（项目自带 `mvnw.cmd`，无需单独安装）

### 启动步骤

**方式一：Maven Wrapper（推荐）**

```cmd
cd backend
mvnw.cmd spring-boot:run
```

**方式二：双击启动脚本（推荐，部署后使用）**

双击 `启动后端.bat`，会自动在新窗口中启动后端服务。窗口关闭 = 服务停止。

**方式三：命令行运行 jar**

```cmd
java -jar target/LvTongTransportDept-0.0.1-SNAPSHOT.jar
```

> 后端启动端口为 `8090`，SSL 已启用，访问 `https://localhost:8090/api/mobile/health` 确认服务运行。

### 数据库初始化

```sql
CREATE DATABASE lvtong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后导入 `lvtong.sql`。

## Swagger / OpenAPI

启动后访问：

| 文档类型 | 地址 |
|---------|------|
| Swagger UI | https://localhost:8090/swagger-ui.html |
| OpenAPI JSON | https://localhost:8090/api-docs |

> 浏览器访问 HTTPS 时，自签名证书会提示不安全，点击「高级 → 继续前往」即可。
