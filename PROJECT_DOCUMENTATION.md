# 房屋租赁平台项目文档

本仓库是一个完整的“租房运营平台”，包含 Spring Boot 后端与 Vue 3 前端。后端提供 REST API + WebSocket 实时能力；前端提供后台/租客/房东多角色的单页控制台。下文从架构、数据模型、接口、运行与部署等维度做细化说明。

---

## 1. 总览与技术栈

### 1.1 后端

- 语言与框架：Java 17 + Spring Boot 3.5.7
- 组件：Spring Web / Spring Data JPA / Validation / WebSocket
- 数据库：H2 文件数据库（MySQL 模式）
- 接口文档：springdoc-openapi (UI)
- 密码：Spring Security Crypto (BCrypt)

### 1.2 前端

- 运行时：Node.js ^20.19 或 >=22.12
- 框架：Vue 3 + Vite (rolldown-vite)
- 状态：Pinia
- 路由：Vue Router
- UI：Tailwind CSS 4、Headless UI、Heroicons
- 实时：STOMP + SockJS

---

## 2. 目录结构

```
housebackend/
├── HELP.md                      # 现有帮助文档
├── PROJECT_DOCUMENTATION.md     # 本文档
├── pom.xml                      # 后端 Maven 配置
├── src/main/java/...            # 后端源码
├── src/main/resources/...       # application.properties 等
├── front/untitled/              # Vue 3 前端项目
├── upload/                      # 上传媒体目录 (后端对外映射 /uploads/**)
├── data/                        # H2 数据库文件
├── target/                      # Maven 构建产物
└── src/services + src/stores    # 根目录下的遗留前端片段（非当前 Vite 项目）
```

> 说明：当前前端项目实际位于 `front/untitled/`。根目录 `src/services` 和 `src/stores` 是早期/遗留的前端代码片段，不与 Vite 项目直接连接。

---

## 3. 运行与构建

### 3.1 后端

```bash
./mvnw clean install
./mvnw spring-boot:run
```

- 默认端口：`http://localhost:8080`
- OpenAPI UI：`/swagger-ui/index.html`
- H2 Console：`/h2-console`

### 3.2 前端

```bash
cd front/untitled
npm install
npm run dev
```

- 默认端口：`http://localhost:5173`
- 生产构建：`npm run build` → `front/untitled/dist/`

---

## 4. 配置与环境变量

### 4.1 application.properties (后端)

位于 `src/main/resources/application.properties`：

- 数据源：`jdbc:h2:file:./data/house_db;MODE=MYSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- 账号密码：`spring.datasource.username=zijun` / `spring.datasource.password=1129491453`
- JPA：`ddl-auto=update`，`show-sql=true`
- H2 Console：`/h2-console`
- 上传目录：`app.upload-dir=upload`
- 上传限制：`spring.servlet.multipart.max-* = -1`（不限制）

### 4.2 前端环境变量

在 `front/untitled/src/services/httpClient.js` 与 `chatClient.js` 中读取：

- `VITE_API_BASE_URL`：REST API 基础地址；默认 `/api`
- `VITE_WS_BASE_URL`：WebSocket 基础地址；未设置时从 API URL 或 `window.location.origin` 推断

---

## 5. 后端架构与分层

### 5.1 入口

- `HousebackendApplication.java`：Spring Boot 启动入口。

### 5.2 配置层 (config)

- `AppConfig`：BCrypt 密码编码器。
- `WebConfig`：静态资源映射，`/uploads/**` 指向 `app.upload-dir`。
- `WebSocketConfig`：注册 `/ws` 与 `/ws/sockjs`，消息前缀 `/app`，主题 `/topic`。
- `StompAuthChannelInterceptor`：连接时读取 `X-Auth-Token`，写入 `sessionAttributes`。
- `DataInitializer`：初始化演示账号、地区、地铁、房源、公告、订单、认证数据。

### 5.3 控制器 (controller)

REST 控制器划分：

- `AuthController`：注册、登录、注销
- `UserController`：用户资料、管理员用户管理
- `HouseController`：房源管理与收藏
- `LocationController`：地区/地铁管理
- `ContactController`：联系记录（看房请求）
- `ChatController`：联系聊天记录
- `SupportTicketController`：客服工单
- `RentalOrderController`：租赁订单
- `CertificationController`：房东认证
- `AnnouncementController`：系统公告
- `StatisticsController`：运营统计
- `MediaController`：媒体上传

WebSocket 控制器：

- `ChatWebSocketController`：/app/contacts/{id}/messages
- `SupportWebSocketController`：/app/support/{id}/messages

### 5.4 服务层 (service)

- `AuthService`：注册/登录/令牌校验，7 天过期，UUID token。
- `UserService`：资料更新、密码修改、角色查询。
- `HouseService`：发布/更新/删除/搜索/收藏/推荐。
- `LocationService`：地区与地铁 CRUD。
- `ContactService`：看房联系、状态更新、确保聊天记录存在。
- `ChatService`：聊天消息持久化 + 权限校验。
- `RentalOrderService`：订单创建/状态流转/合同/终止流程。
- `CertificationService`：认证提交/审核/房东角色升级。
- `SupportService`：客服工单 + 消息 + 通知管理员。
- `AnnouncementService`：公告 CRUD。
- `StatisticsService`：30 日订单趋势、区域/地铁分布。
- `FileStorageService`：媒体存储与类型识别。

### 5.5 数据访问层 (repository)

基于 Spring Data JPA，包含部分查询逻辑：

- `HouseRepository.search(...)`：按地区/地铁/状态筛选
- `RentalOrderRepository.hasOverlappingOrders(...)`：订单时间冲突检测
- `ContactRecordRepository.findWithRelationsById(...)`：聊天关联数据加载
- `SupportMessageRepository.findWithRelationsById(...)`：客服消息关联加载

---

## 6. 数据模型与字段说明

### 6.1 基础实体

- `BaseEntity`
  - `id`、`createdAt`、`updatedAt`
  - 自动维护创建/更新时间

### 6.2 用户与权限

- `User`
  - 账号信息：`username`、`password`
  - 资料信息：`fullName`、`avatarUrl`、`email`、`phone`、`gender`、`bio`、`idNumber`
  - 角色：`role`（`USER` / `LANDLORD` / `ADMIN`）
  - 状态：`status`（`ACTIVE` / `SUSPENDED` / `DELETED`）
  - 标签：`tags`
  - 媒体集：`media` → `UserMedia`
  - `lastLoginAt`

- `UserMedia`
  - `mediaType`（`IMAGE` / `VIDEO`）
  - `url` / `coverUrl` / `description` / `sortOrder`
  - 归属 `user`

### 6.3 房源

- `House`
  - 基本信息：`title`、`description`、`rentPrice`、`deposit`
  - 规格：`area`、`layout`、`orientation`
  - 位置：`address`、`region`、`subwayLine`
  - 时间：`availableFrom`
  - 状态：`status`（`DRAFT` / `PUBLISHED` / `RENTED` / `OFFLINE`）
  - 推荐：`recommended`
  - 配套：`amenities`
  - 媒体集：`media` → `HouseMedia`

- `HouseMedia`
  - `mediaType` / `url` / `coverUrl` / `description` / `sortOrder`
  - 归属 `house`

- `HouseFavorite`
  - 用户收藏关系：`user` + `house`

### 6.4 地区与地铁

- `Region`
  - `name` / `description`

- `SubwayLine`
  - `lineName` / `stationName`
  - 归属 `region`

### 6.5 联系记录与聊天

- `ContactRecord`
  - 房源：`house`
  - 双方：`tenant` / `landlord`
  - `message`（初始留言）
  - `preferredVisitTime`
  - `status`（`PENDING` / `ACCEPTED` / `REJECTED` / `COMPLETED` / `CANCELLED`）
  - `handledAt` / `remarks`

- `ChatMessage`
  - `contactRecord`
  - 发送人：`sender` + `senderRole`
  - 内容：`content`
  - 图片：`imageUrls`

### 6.6 租赁订单

- `RentalOrder`
  - 关联：`house` / `tenant` / `landlord`
  - 租期：`startDate` / `endDate`
  - 费用：`monthlyRent` / `deposit`
  - 状态：`status`（`PENDING` / `CONFIRMED` / `ACTIVE` / `CANCELLED` / `COMPLETED` / `TERMINATED`）
  - 合同：`contractUrl`
  - 终止状态：`terminationStatus`（`NONE` / `REQUESTED` / `APPROVED` / `REJECTED`）
  - 终止字段：`terminationRequester` / `terminationResolver`
  - 终止时间：`terminationRequestedAt` / `terminationResolvedAt`
  - 终止理由：`terminationReason` / `terminationFeedback`

### 6.7 认证与公告

- `LandlordCertification`
  - 申请人：`user`
  - 资料：`documentUrl`（首个）/ `documentUrls`（列表）
  - 状态：`PENDING` / `APPROVED` / `REJECTED`
  - 审核：`reviewedAt` / `reviewedBy`
  - 备注：`reason`

- `SystemAnnouncement`
  - `title` / `content` / `pinned`
  - 创建人：`createdBy`

### 6.8 客服工单

- `SupportTicket`
  - `subject` / `category`
  - 状态：`OPEN` / `IN_PROGRESS` / `RESOLVED` / `CLOSED`
  - 发起人：`requester`
  - 处理人：`handler`
  - `latestMessage` / `closedAt`

- `SupportMessage`
  - `ticket`
  - `sender` + `senderRole`
  - `content`
  - `attachmentUrls`

### 6.9 认证令牌

- `AuthToken`
  - `token`（UUID）
  - `user`
  - `expiresAt`
  - `revoked`

---

## 7. 核心业务逻辑要点

- 登录生成 `AuthToken`，通过 `X-Auth-Token` 在 REST 与 STOMP 连接中传递。
- 房源发布前校验房东认证状态；未认证不可发布。
- 创建订单时检测时间冲突（状态为 `CONFIRMED` / `ACTIVE` 的订单）。
- 终止订单采用状态机：`REQUESTED` → `APPROVED/REJECTED`。
- 聊天与客服消息支持文本 + 图片/附件；空消息会被拒绝。
- 客服工单由管理员接手后状态变为 `IN_PROGRESS`，关闭/解决时记录 `closedAt`。

---

## 8. REST API 概览

> 需要登录的接口必须携带 `X-Auth-Token`。

### 8.1 认证

- `POST /api/auth/register` 注册（禁止注册 ADMIN）
- `POST /api/auth/login` 登录
- `POST /api/auth/logout` 注销

### 8.2 用户

- `GET /api/users/me` 当前用户资料
- `PUT /api/users/me` 更新资料（含媒体）
- `POST /api/users/me/password` 修改密码
- `GET /api/users/{id}` 查看其他用户
- `GET /api/users?role=...` 管理员按角色筛选
- `POST /api/users` 管理员创建用户
- `DELETE /api/users/{id}` 管理员删除用户

### 8.3 房源

- `GET /api/houses/search` 搜索（regionId/subwayId/status + page/size）
- `GET /api/houses/latest` 最新房源
- `GET /api/houses/recommended` 推荐房源
- `GET /api/houses/random?size=4` 随机推荐
- `GET /api/houses/{id}` 详情
- `GET /api/houses/mine` 我的房源（房东）
- `POST /api/houses` 发布
- `PUT /api/houses/{id}` 更新
- `DELETE /api/houses/{id}` 删除（要求无订单）
- `POST /api/houses/{id}/favorite` 收藏开关
- `GET /api/houses/favorites` 我的收藏
- `GET /api/houses/favorites/all` 全部收藏（管理员）
- `POST /api/houses/{id}/recommend` 设置推荐（管理员）
- `POST /api/houses/{id}/status?status=...` 修改状态（管理员）

### 8.4 地区/地铁

- `GET /api/locations/regions` 地区列表
- `POST /api/locations/regions` 创建（管理员）
- `PUT /api/locations/regions/{id}` 更新（管理员）
- `DELETE /api/locations/regions/{id}` 删除（管理员）
- `GET /api/locations/subways` 地铁列表
- `POST /api/locations/subways` 创建（管理员）
- `PUT /api/locations/subways/{id}` 更新（管理员）
- `DELETE /api/locations/subways/{id}` 删除（管理员）

### 8.5 联系与聊天

- `POST /api/contacts` 创建联系（租客）
- `POST /api/contacts/ensure` 确保存在联系（租客）
- `GET /api/contacts/mine` 租客联系
- `GET /api/contacts/landlord` 房东联系
- `GET /api/contacts` 全部记录（管理员）
- `POST /api/contacts/{id}/status` 房东更新状态
- `POST /api/chat/contacts/{id}/messages` 发送消息
- `GET /api/chat/contacts/{id}/messages` 聊天记录

### 8.6 租赁订单

- `POST /api/orders` 创建订单（租客）
- `GET /api/orders/mine` 租客订单
- `GET /api/orders/landlord` 房东订单
- `GET /api/orders` 管理员订单
- `POST /api/orders/{id}/cancel` 租客取消
- `POST /api/orders/{id}/confirm` 房东确认
- `POST /api/orders/{id}/activate` 房东激活
- `POST /api/orders/{id}/contract` 上传合同
- `GET /api/orders/{id}/contract` 下载合同
- `POST /api/orders/{id}/terminate` 发起终止
- `POST /api/orders/{id}/termination/approve` 同意终止
- `POST /api/orders/{id}/termination/reject` 驳回终止

### 8.7 认证/公告/统计/媒体

- `POST /api/certifications` 提交认证
- `GET /api/certifications/me` 查看个人认证
- `GET /api/certifications` 认证列表（管理员）
- `POST /api/certifications/{id}/review` 审核认证（管理员）

- `GET /api/announcements` 公告分页
- `GET /api/announcements/latest` 最新公告
- `POST /api/announcements` 创建公告（管理员）
- `PUT /api/announcements/{id}` 更新公告（管理员）
- `DELETE /api/announcements/{id}` 删除公告（管理员）

- `GET /api/statistics` 运营统计（管理员）

- `POST /api/media/upload` 上传文件（multipart/form-data）

---

## 9. WebSocket / STOMP

- 端点：
  - `/ws`
  - `/ws/sockjs`
- 连接认证：在 `CONNECT` 帧中传 `X-Auth-Token`。
- 应用端点：
  - `/app/contacts/{contactId}/messages`
  - `/app/support/{ticketId}/messages`
- 主题订阅：
  - `/topic/contacts/{contactId}` 聊天消息
  - `/topic/support/{ticketId}` 客服消息
  - `/topic/users/{userId}` 用户通知
  - `/topic/admin/certifications` 认证申请提醒
  - `/topic/admin/support` 客服工单提醒

---

## 10. 前端应用说明 (front/untitled)

### 10.1 入口与路由

- 入口：`front/untitled/src/main.js`，启动 Pinia + Router。
- 路由：`front/untitled/src/router/index.js`，包含角色校验与登录重定向。

主要路由示例：

- `/dashboard` 仪表盘（所有角色）
- `/houses/explore` 房源广场
- `/houses/mine` 房东房源
- `/houses/admin` 管理员房源管理
- `/contacts/mine|landlord|admin` 联系记录
- `/support/center` 客服中心（USER/LANDLORD）
- `/support/admin` 客服工单（ADMIN）
- `/orders/mine|landlord|admin` 订单
- `/announcements` 公告
- `/certification/apply` 认证申请
- `/certification/admin` 认证审核
- `/statistics` 统计
- `/users/admin` 用户管理
- `/locations/admin` 地区/地铁

### 10.2 Store 与客户端

Pinia store 主要文件：

- `stores/auth.js`：登录、注册、用户资料、token 持久化
- `stores/chatNotifications.js`：聊天会话、未读消息管理
- `stores/support.js`：客服面板状态
- `stores/adminAlerts.js`：管理员提醒

网络层：

- `services/httpClient.js`：Axios 封装，自动加入 `X-Auth-Token`
- `services/apiClient.js`：REST API 封装
- `services/chatClient.js`：STOMP/SockJS 客户端封装

UI 组件：

- `ChatPanelHost.vue` / `HouseChatPanel.vue`：聊天面板
- `SupportPanelHost.vue` / `SupportChatPanel.vue`：客服面板
- `AdminCertificationToasts.vue`：管理员提醒

---

## 11. 数据初始化与演示账号

`DataInitializer` 启动时会创建以下演示账号（若不存在）：

- `superadmin` / `123456`（ADMIN）
- `landlord` / `123456`（LANDLORD）
- `tenant` / `123456`（USER）

同时初始化：

- 地区与地铁线路
- 三套演示房源
- 系统公告
- 演示订单
- 房东认证（默认通过）

---

## 12. 文件上传与静态资源

- 上传入口：`POST /api/media/upload`
- 存储路径：`app.upload-dir`（默认 `upload/`）
- 访问路径：`/uploads/<filename>`
- 文件名格式：`yyyyMMddHHmmss-<uuid>.<ext>`
- 允许的类型：image/* 或 video/*（未匹配类型会拒绝）

---

## 13. 部署建议

### 13.1 后端

```bash
./mvnw clean package -DskipTests
java -jar target/housebackend-0.0.1-SNAPSHOT.jar
```

- 生产环境建议：
  - 替换 H2 → MySQL/PostgreSQL
  - 调整上传目录与大小限制
  - 反向代理暴露 `/api`、`/uploads`、`/ws`

### 13.2 前端

```bash
cd front/untitled
npm run build
```

将 `dist/` 发布到静态服务器，并配置 SPA 路由 fallback（全部路由重写到 `index.html`）。

---

## 14. 可继续完善的方向（可选）

- 引入 Spring Security 统一认证过滤，替换控制器内 `requireUser` 方式。
- 支持订单支付与退款流程。
- 将上传文件迁移至对象存储（OSS/S3）。
- 增加审计日志与操作记录。

