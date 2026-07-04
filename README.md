# 多聊天室群聊系统

面向投研投顾直播间场景的多聊天室群聊系统。支持多聊天室并行管理、RBAC 权限、管理员广播、用户发言异步审核后展示、WebSocket 实时推送与运行监控。

> 总体技术方案见 [`技术方案.md`](./技术方案.md)；详细设计文档见 [`docs/`](./docs/) 目录。

## 文档索引

| 文档 | 说明 |
|------|------|
| [技术方案.md](./技术方案.md) | 总体方案、模块划分、实施计划 |
| [docs/架构设计优化说明.md](./docs/架构设计优化说明.md) | 落地后的架构优化（WS 单例、登录态统计、RBAC、二级缓存等） |
| [docs/数据库设计.md](./docs/数据库设计.md) | ER 图、表结构、Redis 键、Flyway 版本 |
| [docs/接口与WebSocket协议说明.md](./docs/接口与WebSocket协议说明.md) | 完整 REST API（44 个接口）与 STOMP 协议 |

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2 · JDK 21 · Spring WebSocket(STOMP) · MyBatis-Plus · JWT · Flyway |
| 数据库 | MySQL 8（默认）/ PostgreSQL 14+ |
| 缓存/队列 | Redis 6+ · Guava L1 + Redis L2 二级缓存 |
| 前端 | Vue 3 · Vite · Pinia · Element Plus · @stomp/stompjs |
| 监控 | Actuator · Micrometer · Prometheus |

## 目录结构

```
Chat/
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/jin/chat/
│       ├── config/              # WebSocket / Redis / MyBatis / 监控 / 缓存 配置
│       ├── controller/          # REST 控制器（11 个）
│       ├── ws/                  # STOMP 拦截器、握手认证、事件监听
│       ├── service/(impl)       # 业务逻辑
│       ├── mapper/              # MyBatis Mapper
│       ├── domain/              # entity / enums / ao / vo / dto / query
│       ├── repository/          # Redis 队列、会话、登录态、缓存
│       ├── common/              # 缓存、锁、拦截器、异常、工具
│       └── task/                # 审核超时扫描
│   └── src/main/resources/db/migration/{mysql|postgresql}/
├── frontend/                    # Vue 3 前端
│   └── src/{views,stores,api,ws,router,utils}
├── docs/                        # 设计文档（架构 / 数据库 / API·WS）
├── docker-compose.yml           # Redis（及可选 PostgreSQL）
└── 技术方案.md
```

## 数据库切换（MySQL / PostgreSQL）

- `application.yml` 中 `spring.profiles.active`：`mysql`（默认）或 `postgres`
- 数据源：`application-mysql.yml` / `application-postgres.yml`
- Flyway：`db/migration/{vendor}/` 自动选择迁移脚本

## 快速开始

### 1. 启动依赖

- **MySQL**：本地 `localhost:3306`，默认 `root/root`，库名 `chat`（可自动创建）
- **Redis**：`docker compose up -d redis`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

Flyway 自动建表；`AdminInitializer` 创建默认管理员 **admin / admin123**（仅开发环境使用，登录页不展示）。  
端口 `8080`，Prometheus：`http://localhost:8080/actuator/prometheus`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`

## 系统功能概览

| 模块 | 功能 |
|------|------|
| 认证 | 注册、登录、退出；JWT 有效期可配置；重置密码强制下线 |
| 工作台 | 快捷入口、我的聊天室、指标概览（含在线人数） |
| 聊天室 | CRUD、状态流转、开放/审批加入、人员管理（拉人/踢人/审批） |
| 消息 | 用户提交 → 审核 → WebSocket 实时展示；历史游标分页 |
| 审核 | 待审核列表、单条/批量通过拒绝、超时自动拒绝 |
| 广播 | 管理员多房间广播、系统通知（绕过审核） |
| RBAC | 用户/角色/菜单管理，多角色多菜单，前后端路径鉴权 |
| 全局配置 | 审核超时、消息长度、JWT 有效期等运行期可调 |
| 监控 | 在线人数、审核队列、延迟分位数、错误率、连接池 |

## 核心流程

- **注册/登录**：注册默认 `USER` 角色；登录写入 Redis 登录态（**在线人数**统计口径）。
- **聊天室**：管理员创建；用户浏览、加入（开放或审批）；管理员可人员管理。
- **发言审核**：提交 → Redis 待审核队列 → 管理员审核 → 通过后 WebSocket 推送到 `/topic/room.{id}`；超时通知本人。
- **实时通信**：登录后在主布局建立全局 WebSocket 单例；房间页订阅 Topic，审核页订阅 `/topic/audit`。

## 关键设计

- **WebSocket 单连接多路复用**：全局 WS + `/topic/room.{roomId}` / `/user/queue/notifications` / `/topic/audit`
- **在线人数**：统计登录成功且未退出/未过期的用户数（非 WS 连接数）
- **审核并发安全**：乐观锁 CAS + 分布式锁 + 事务后推送
- **RBAC + 二级缓存**：Guava L1 + Redis L2 + Pub/Sub 失效
- **at-least-once 推送**：客户端 `messageId` 去重 + 历史补齐

## 测试

```bash
cd backend
mvn test          # 单元测试
mvn verify        # 测试 + JaCoCo 行覆盖率 ≥ 80%
```

报告：`backend/target/site/jacoco/index.html`

## AI 辅助说明

本项目的方案设计、代码骨架、文档在 AI 辅助下生成，关键架构决策与业务规则经人工核对确认。
