# 多聊天室群聊系统

面向投研投顾直播间场景的多聊天室群聊系统。支持多聊天室并行管理、管理员广播、用户发言异步审核后展示、WebSocket 实时推送。

> 完整技术方案见 [`技术方案.md`](./技术方案.md)。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2 · Spring WebSocket(STOMP) · MyBatis-Plus · JWT · Flyway |
| 数据库 | PostgreSQL 14+ |
| 缓存/队列 | Redis 6+ |
| 前端 | Vue 3 · Vite · Pinia · Element Plus · @stomp/stompjs |
| 监控 | Actuator · Micrometer · Prometheus |

## 目录结构

```
Chat/
├── backend/                     # Spring Boot 后端（controller / service / mapper 三层）
│   ├── src/main/java/com/chat/
│   │   ├── config/              # WebSocket / Redis / MyBatis-Plus / 安全 / 异步 / 监控 配置
│   │   ├── controller/          # REST 控制器
│   │   ├── ws/                  # STOMP 拦截器、事件监听、连接身份
│   │   ├── service/(impl)       # 业务逻辑
│   │   ├── mapper/              # MyBatis Mapper 接口
│   │   ├── domain/              # entity / enums / ao / vo / dto / query
│   │   ├── repository/          # Redis 队列与会话封装
│   │   ├── task/                # 审核超时扫描任务
│   │   └── common/              # 统一返回、异常、常量、工具、上下文、拦截器
│   └── src/main/resources/
│       ├── mapper/*.xml
│       ├── db/migration/V1__init.sql
│       └── application.yml
├── frontend/                    # Vue 3 前端
│   └── src/{views,stores,api,ws,router}
├── docs/                        # 设计文档
├── docker-compose.yml           # PostgreSQL + Redis
└── 技术方案.md
```

## 数据库切换（MySQL / PostgreSQL）

项目通过 Spring Profile + Flyway `{vendor}` 占位符支持两种数据库，**当前默认 `mysql`**（本地测试用）：

- `application.yml` 中 `spring.profiles.active` 控制使用哪套数据源：`mysql` 或 `postgres`。
- 数据源配置分别在 `application-mysql.yml` / `application-postgres.yml`，按需修改账号密码。
- 迁移脚本按数据库分目录：`db/migration/mysql/` 与 `db/migration/postgresql/`，Flyway 依据 `{vendor}` 自动选择。
- MyBatis-Plus 分页插件自动识别数据库类型，无需改代码。

> 本地 MySQL 测试无误后，切回 PostgreSQL 只需将 `spring.profiles.active` 改为 `postgres`。

## 快速开始

### 1. 启动依赖

- **MySQL（默认）**：使用本地已安装的 MySQL。默认连接 `localhost:3306`，账号 `root/root`（可在 `application-mysql.yml` 修改）；JDBC URL 含 `createDatabaseIfNotExist=true`，会自动创建 `chat` 库。
- **Redis**：应用仍依赖 Redis（会话/审核队列）。可用 Docker 单独启动：

```bash
docker compose up -d redis
```

> 若改用 PostgreSQL，可 `docker compose up -d`（含 postgres + redis），并把 profile 切为 `postgres`。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动时 Flyway 会自动建表，`AdminInitializer` 会创建默认管理员账号 **admin / admin123**。
后端默认端口 `8080`，监控端点 `http://localhost:8080/actuator/prometheus`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

## 核心流程

- **注册/登录**：普通用户注册后角色为 `USER`；使用 admin 登录获得管理员能力。
- **聊天室**：管理员创建聊天室（开放/需审批）；用户浏览、加入。
- **发言审核**：用户在已加入的聊天室提交消息 → 进入待审核队列（Redis）→ 管理员在审核面板通过/拒绝 → 通过后按提交时间顺序 WebSocket 推送到房间；超时（默认 30s）自动拒绝并通知本人。
- **广播/紧急通知**：管理员可绕过审核直接向一个或多个聊天室发送系统通知。

## 关键设计

- **WebSocket 单连接多路复用**：`/topic/room.{roomId}` 订阅房间、`/user/queue/notifications` 个人通知、`/topic/audit` 管理员待审核。
- **审核状态并发安全**：`messages` 表 `version` 乐观锁 + `updateStatusCas`（仅当 `PENDING_REVIEW` 时更新），保证并发审核/超时只生效一次。
- **at-least-once 推送**：客户端按 `messageId` 去重，断线重连后拉取历史补齐。
- **审核服务无状态**，可水平扩展；超时任务多实例部署时应加分布式锁（如 ShedLock）。

## 测试

```bash
cd backend
mvn test
```

## AI 辅助说明

本项目的方案设计、代码骨架、文档在 AI 辅助下生成，关键架构决策与业务规则经人工核对确认。
