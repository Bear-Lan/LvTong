# 绿通交通系统 - 前端

基于 Vue 3 + Vite 的后台管理系统前端，提供用户认证和车辆查验记录管理界面。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 响应式前端框架 |
| 构建 | Vite 8 | 快速的开发服务器和打包工具 |
| UI | Element Plus | Vue 3 组件库 |
| 状态 | Pinia | 轻量级状态管理 |
| 路由 | Vue Router 4 | SPA 路由 + 导航守卫 |
| HTTP | Axios | HTTP 请求库（封装于 request.js） |
| 图标 | @element-plus/icons-vue | Element Plus 官方图标（全局注册） |

## 项目结构

```
src/
├── api/                                         # API 接口封装层
│   ├── user.js                                 # 用户模块接口（登录/注册/CRUD/用户组）
│   └── vehicleInspection.js                     # 查验模块接口（CRUD+多条件查询+看板）
│
├── components/                                  # 公共组件
│   └── MainLayout.vue                          # 主布局（已被 layout/ 替代）
│
├── layout/                                      # 布局组件
│   └── Layout.vue                              # 主布局（侧边栏+顶部栏+内容区）
│
├── router/                                     # 路由配置
│   └── index.js                               # 路由定义 + 路由守卫（登录拦截）
│
├── stores/                                     # Pinia 状态管理
│   └── user.js                                 # 用户状态（token、用户信息、登录/登出）
│
├── utils/                                      # 工具函数
│   └── request.js                             # Axios 封装（请求拦截、Token注入、响应拦截）
│
├── views/                                      # 页面视图
│   ├── Login.vue                              # 登录页
│   ├── Register.vue                           # 注册页
│   ├── Dashboard.vue                          # 首页概览（统计卡片+查验统计）
│   ├── UserManagement.vue                     # 用户管理（含最后登录时间、排序）
│   ├── UserGroup.vue                          # 用户组管理
│   ├── HistoricalRecords.vue                  # 车辆查验记录（主页面）
│   ├── InspectionDetail.vue                   # 查验详情弹窗
│   ├── InspectionEdit.vue                     # 新增/编辑弹窗
│   └── VehicleInspection.vue                   # 旧版查验页（已废弃）
│
├── App.vue                                      # 根组件
└── main.js                                     # 应用入口（全局注册图标、Pinia、Router）
```

## 页面路由

| 路径 | 页面 | 权限 | 说明 |
|------|------|------|------|
| `/login` | Login.vue | 公开 | 登录页 |
| `/register` | Register.vue | 公开 | 注册页 |
| `/dashboard` | Dashboard.vue | 需登录 | 首页统计看板 |
| `/users` | UserManagement.vue | 需登录 | 用户管理 |
| `/groups` | UserGroup.vue | 需登录 | 用户组管理 |
| `/inspection` | HistoricalRecords.vue | 需登录 | 车辆查验记录 |

**路由守卫逻辑：**
- 未登录访问需认证页面 → 自动跳转 `/login`
- 已登录访问 `/login` → 自动跳转 `/dashboard`

## 功能清单

### 用户认证
- [x] 登录（图形验证码，5分钟有效）
- [x] 注册（用户名唯一性校验）
- [x] JWT Token 自动注入（请求拦截器）
- [x] Token 过期自动跳转登录（401 响应拦截）
- [x] 退出登录

### 用户管理 `/users`
- [x] 用户列表（分页+排序）
- [x] 新增用户（表单验证）
- [x] 编辑用户（支持部分字段更新）
- [x] 删除用户（二次确认弹窗）
- [x] 启用/禁用状态切换
- [x] 重置密码
- [x] 最后登录时间显示（相对时间：刚刚/分钟前/小时前）
- [x] 排序（默认/登录时间↑↓/用户组/角色）

### 用户组管理 `/groups`
- [x] 用户组列表
- [x] 新增用户组

### 车辆查验记录 `/inspection`

**列表页（HistoricalRecords.vue）**
- [x] 统计看板（今日查验/合格/不合格/绿通）
- [x] 分页表格（斑马条纹+行悬停高亮）
- [x] 多条件搜索（车牌模糊/司机电话精确/操作员/查验结果/时间范围）
- [x] 查询/重置按钮
- [x] 满载率颜色分级显示（≥80%绿/≥60%橙/<灰色）
- [x] 查验结果标签（合格绿/不合格红）
- [x] 点击行高亮选中

**详情弹窗（InspectionDetail.vue）**
- [x] 证据链照片（5列网格：车头照/车尾照/行驶证/货物照/车侧照）
- [x] 核心影像（2列：透视影像+车身照片）
- [x] 数据网格（4列：站点/车辆/通行/财务）
- [x] 查验结论（合格大字/不合格原因/操作员信息）
- [x] 图片点击放大预览（el-image preview）
- [x] 金额自动÷100 转换显示（数据库存"分"，页面显示"元"）

**编辑弹窗（InspectionEdit.vue）**
- [x] 新增模式（清空表单）
- [x] 编辑模式（填充原值）
- [x] 四区块分组表单（基本信息/货物信息/查验信息/通行码信息）
- [x] 车牌必填校验
- [x] 提交时过滤 null 字段（部分更新语义）
- [x] 操作成功自动刷新列表

## 核心模块说明

### Axios 封装（request.js）

```
请求流程：
  组件调用 API 方法（如 getInspectionList）
    → request.js 创建 axios 实例（baseURL=/api，即通过 Nginx 代理）
    → 请求拦截器：读取 Pinia token，注入 Authorization: Bearer xxx
    → 发送请求到后端
    → 响应拦截器：
        - code=200：直接返回 data
        - code≠200：ElMessage.error() 提示 + Promise.reject()
        - 401 未授权：ElMessage.error() + 跳转 /login
```

### Pinia 状态管理（stores/user.js）

```js
// 状态
token: ''           // JWT Token
userInfo: {}         // 当前登录用户信息

// 方法
login(token, userInfo)  // 登录成功写入状态，持久化到 localStorage
logout()             // 清除状态和 localStorage
isAuthenticated()    // 判断是否已登录
```

### 图片路径转换（InspectionDetail.vue）

数据库存储本地路径如 `D:\photos\car.jpg`，浏览器无法直接访问。
`formatImageUrl()` 自动转换为 `file:///D:/photos/car.jpg`：

```js
const formatImageUrl = (path) => {
  if (!path) return ''
  return path.replace(/^D:/i, 'file:///D:').replace(/\\/g, '/')
}
```

### 金额单位转换

数据库 `passcode_fee` 字段存储单位为"分"（元×100），前端展示时÷100：

```js
// 125000（分）→ 1250.00 元
(Number(row.passcodeFee) / 100).toFixed(2) + ' 元'
```

## 与后端接口对应

### 用户接口

| 前端方法 | HTTP | 请求路径 | 说明 |
|---------|------|---------|------|
| `login(data)` | POST | /api/user/login | 登录 |
| `register(data)` | POST | /api/user/register | 注册 |
| `getCaptcha()` | GET | /api/user/captcha | 图形验证码 |
| `getUserList(params)` | GET | /api/user/list | 用户列表 |
| `getUserInfo(id)` | GET | /api/user/{id} | 用户详情 |
| `updateUser(id, data)` | PUT | /api/user/{id} | 更新用户 |
| `deleteUser(id)` | DELETE | /api/user/{id} | 删除用户 |
| `resetPassword(id)` | POST | /api/user/{id}/reset-password | 重置密码 |

### 车辆查验接口

| 前端方法 | HTTP | 请求路径 | 说明 |
|---------|------|---------|------|
| `getInspectionList(params)` | GET | /api/inspection/list | 分页+多条件查询 |
| `getInspectionById(id)` | GET | /api/inspection/{id} | 单条详情 |
| `createInspection(data)` | POST | /api/inspection | 新增 |
| `updateInspection(id, data)` | PUT | /api/inspection/{id} | 部分更新 |
| `deleteInspection(id)` | DELETE | /api/inspection/{id} | 删除 |
| `getDashboard()` | GET | /api/inspection/dashboard | 统计看板 |
| `getProducts()` | GET | /api/inspection/products | 农产品品种 |

### 用户组接口

| 前端方法 | HTTP | 请求路径 |
|---------|------|---------|
| `getGroupList()` | GET | /api/groups |
| `getGroupById(id)` | GET | /api/groups/{id} |
| `createGroup(data)` | POST | /api/groups |

## 运行项目

```cmd
cd frontend
npm install          # 安装依赖
npm run dev          # 开发模式（http://localhost:5173）
npm run build        # 构建生产版本（输出到 dist/）
```

**前后端联调：**

- 后端启动：`java -jar LvTongTransportDept-0.0.1-SNAPSHOT.jar`（端口 **8090**，HTTPS）
- 前端开发：`npm run dev`（端口 5173）
- Vite 代理：`/api/*` 请求自动转发至 `https://localhost:8090/api/*`

> 开发模式下 Vite 会忽略后端自签名证书，但生产环境通过 Nginx 部署时，由 Nginx 配置 `proxy_ssl_verify off` 处理。

## 图片预览说明

详情弹窗支持以下图片类型的本地预览：

| 图片类型 | 数据库字段 | 说明 |
|---------|-----------|------|
| 车头照 | `head_image_path` | 车辆前方全景 |
| 车尾照 | `tail_image_path` | 车辆后方全景 |
| 行驶证 | `license_image_path` | 行驶证正本 |
| 货物照 | `goods_image_path` | 车厢内货物 |
| 车侧照 | `body_image_path` | 车辆侧面 |
| 透视影像 | `transparent_image_path` | ETC 通道 X 光扫描图 |
| 通行凭证 | `passcode_image_path` | ETC/CPC 卡照片 |
| 车顶照 | `top_image_path` | 车厢顶部 |

> 图片路径存储在数据库中，前端通过 `formatImageUrl()` 转换为 `file:///` URL 后使用 el-image 组件预览。
