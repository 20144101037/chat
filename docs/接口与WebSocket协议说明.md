# 主要 API 接口设计与 WebSocket 消息协议

> 后端包路径：`com.jin.chat`，统一前缀 `/api`。  
> 详细架构背景见 [`架构设计优化说明.md`](./架构设计优化说明.md)。

---

## 一、通用约定

### 1.1 统一响应体

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "traceId": "abc123",
  "timestamp": 1710000000000
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 非 0 | 业务/系统错误，`message` 为可读说明 |

### 1.2 认证

| 场景 | 方式 |
|------|------|
| REST | Header：`Authorization: Bearer <jwt>` |
| WebSocket | 握手 URL：`/ws?token=<jwt>`（SockJS） |
| 公开接口 | 仅 `POST /api/auth/register`、`POST /api/auth/login` 无需 Token |

### 1.3 权限模型

| 级别 | 说明 | 典型接口 |
|------|------|----------|
| 公开 | 无需登录 | 注册、登录 |
| 登录用户 | 有效 JWT | 加入房间、发消息、我的菜单 |
| 管理员角色 | `LoginUser.role` 为 `ROOM_ADMIN` 或 `SYS_ADMIN` | 聊天室 CRUD、成员管理 |
| 菜单 RBAC | 用户角色被授权对应**菜单路径** | 审核 `/app/audit`、广播 `/app/broadcast`、系统管理等 |

> 自定义角色（如「业务管理员」）通过 RBAC 分配菜单即可访问对应功能，无需改代码。

### 1.4 分页

列表接口统一使用：

| 参数 | 说明 |
|------|------|
| pageNo | 页码，从 1 开始，默认 1 |
| pageSize | 每页条数，默认 10 |

响应 `PageResult`：

```json
{
  "total": 100,
  "pageNo": 1,
  "pageSize": 10,
  "records": [ ]
}
```

---

## 二、REST API 一览

### 2.1 认证 `/api/auth`

| 方法 | 路径 | 请求体 | 响应 | 权限 |
|------|------|--------|------|------|
| POST | `/api/auth/register` | `{ username, password, nickname? }` | `UserVO` | 公开 |
| POST | `/api/auth/login` | `{ username, password }` | `LoginVO { token, user }` | 公开 |
| POST | `/api/auth/logout` | 无 | `Void` | 登录 |

**LoginVO.user 字段**：`id`, `username`, `nickname`, `role`, `status`

---

### 2.2 当前用户 `/api/me`

| 方法 | 路径 | 参数 | 响应 | 权限 |
|------|------|------|------|------|
| GET | `/api/me/menus` | 无 | `List<MenuVO>` 树形菜单 | 登录 |
| GET | `/api/me/messages` | `status?`, `pageNo`, `pageSize` | `PageResult<MessageVO>` | 登录 |

---

### 2.3 聊天室 `/api/rooms`

| 方法 | 路径 | 请求 | 响应 | 权限 |
|------|------|------|------|------|
| POST | `/api/rooms` | `RoomCreateAO`: name, description?, maxUsers?, joinPolicy(OPEN/APPROVAL) | `RoomVO` | 管理员 |
| PUT | `/api/rooms/{id}` | `RoomUpdateAO`: name?, description?, maxUsers?, joinPolicy?, status? | `RoomVO` | 管理员 |
| DELETE | `/api/rooms/{id}` | 无 | `Void` | 管理员 |
| PATCH | `/api/rooms/{id}/status` | `{ status: ACTIVE\|PAUSED\|CLOSED }` | `Void` | 管理员 |
| GET | `/api/rooms` | `keyword?`, `status?`, `pageNo`, `pageSize` | `PageResult<RoomVO>` | 登录 |
| GET | `/api/rooms/{id}` | 无 | `RoomVO` | 登录 |
| GET | `/api/rooms/mine` | 无 | `List<RoomVO>` | 登录 |
| POST | `/api/rooms/broadcast` | `{ roomIds: [], content }` | `Void` | 菜单 `/app/broadcast` |
| POST | `/api/rooms/{id}/system-notify` | `{ content }` | `Void` | 管理员（绕过审核） |

**RoomVO 主要字段**：`id`, `name`, `description`, `maxUsers`, `joinPolicy`, `status`, `memberCount`, `ownerId`

---

### 2.4 成员 `/api/rooms/{id}`

| 方法 | 路径 | 请求 | 响应 | 权限 |
|------|------|------|------|------|
| POST | `/api/rooms/{id}/join` | 无 | `{ memberStatus }` | 登录 |
| POST | `/api/rooms/{id}/leave` | 无 | `Void` | 登录 |
| GET | `/api/rooms/{id}/members` | `status?` | `List<MemberVO>` | 管理员 |
| POST | `/api/rooms/{id}/members/{userId}/approve` | `{ pass: true/false }` | `Void` | 管理员 |
| POST | `/api/rooms/{id}/members/{userId}/add` | 无 | `Void` | 管理员（直接拉入） |
| DELETE | `/api/rooms/{id}/members/{userId}` | 无 | `Void` | 管理员（踢人，不可踢房主） |
| GET | `/api/rooms/{id}/member-candidates` | **`keyword`**（必填） | `List<MemberCandidateVO>` | 管理员 |

---

### 2.5 消息 `/api/rooms/{roomId}/messages`

| 方法 | 路径 | 请求 | 响应 | 权限 |
|------|------|------|------|------|
| POST | `/api/rooms/{roomId}/messages` | `{ content }`（≤2000 字） | `MessageVO` | 已加入成员 |
| GET | `/api/rooms/{roomId}/messages` | `before?`（游标 id）, `size`（默认 20） | `List<MessageVO>` | 已加入成员（仅 APPROVED） |

**MessageVO 主要字段**：`id`, `roomId`, `senderId`, `senderName`, `content`, `type`, `status`, `submittedAt`, `reviewedAt`

---

### 2.6 审核 `/api/audit`

| 方法 | 路径 | 请求 | 响应 | 权限 |
|------|------|------|------|------|
| GET | `/api/audit/pending` | `roomId?`, `pageNo`, `pageSize` | `PageResult<MessageVO>` | 菜单 `/app/audit` |
| POST | `/api/audit/{messageId}/approve` | 无 | `Void` | 菜单 `/app/audit` |
| POST | `/api/audit/{messageId}/reject` | `{ reason? }` | `Void` | 菜单 `/app/audit` |
| POST | `/api/audit/batch` | `{ messageIds[], action: APPROVE\|REJECT, reason? }` | `Void` | 菜单 `/app/audit` |

审核通过后服务端向 `/topic/room.{roomId}` 推送 `CHAT` 消息；拒绝/超时向发送者 `/user/queue/notifications` 推送 `NOTIFICATION`。

---

### 2.7 全局配置 `/api/configs`

| 方法 | 路径 | 请求 | 响应 | 权限 |
|------|------|------|------|------|
| GET | `/api/configs` | `keyword?`, `configGroup?`, `pageNo`, `pageSize` | `PageResult<SysConfigDO>` | 菜单 `/app/configs` |
| GET | `/api/configs/runtime` | — | `ConfigRuntimeVO` | 登录即可 |
| PUT | `/api/configs/{id}` | `{ configValue }` | `SysConfigDO` | 菜单 `/app/configs` |

**ConfigRuntimeVO**（供创建聊天室、发消息等前端默认值）：

| 字段 | 说明 |
|------|------|
| roomDefaultMaxUsers | 新建聊天室默认最大人数（对应 `room.default-max-users`） |
| messageMaxLength | 单条消息最大字符数（对应 `message.max-length`） |

> 管理端修改 `PUT /api/configs/{id}` 后，后端业务会立即读新值；前端打开「新建聊天室」时会再次请求 `/runtime` 刷新默认值。

---

### 2.8 性能监控 `/api/metrics`

| 方法 | 路径 | 响应 | 权限 |
|------|------|------|------|
| GET | `/api/metrics/dashboard` | `MetricsDashboardVO` | 菜单 `/app/metrics` |

**MetricsDashboardVO 字段**：

| 字段 | 说明 |
|------|------|
| onlineUsers | 在线人数（登录成功且未退出/未过期） |
| auditQueueLength | 待审核队列长度 |
| latencyP95Ms / latencyP99Ms / latencyAvgMs / latencyMaxMs | 审核处理延迟（ms） |
| errorRate / totalRequests / errorRequests | 系统错误率 |
| dbPoolActive / dbPoolIdle / dbPoolMax / dbPoolUsage | 连接池 |
| timestamp | 采样时间 |

Prometheus：`chat.logged-in.users`、`chat.online.users`（同口径）、`chat.audit.pending.size`

---

### 2.9 系统管理 — 用户 `/api/admin/users`

| 方法 | 路径 | 请求 | 权限 |
|------|------|------|------|
| GET | `/api/admin/users` | `keyword?`, `status?`, 分页 | 菜单 `/app/system/users` |
| GET | `/api/admin/users/{id}/roles` | 无 | 同上 |
| PUT | `/api/admin/users/{id}/roles` | `{ ids: [] }` 角色 ID 列表 | 同上 |
| PATCH | `/api/admin/users/{id}/status` | `{ status: ACTIVE\|BANNED }` | 同上 |
| PUT | `/api/admin/users/{id}/password` | `{ password }` | 同上（重置后旧 Token 失效） |

---

### 2.10 系统管理 — 角色 `/api/admin/roles`

| 方法 | 路径 | 请求 | 权限 |
|------|------|------|------|
| GET | `/api/admin/roles` | 分页 + keyword | 菜单 `/app/system/roles` |
| GET | `/api/admin/roles/all` | 无 | 角色/用户管理菜单 |
| GET | `/api/admin/roles/menu-tree` | 无 | 同上（只读菜单树，供分配菜单使用） |
| POST | `/api/admin/roles` | `RoleAO` | 菜单 `/app/system/roles` |
| PUT | `/api/admin/roles/{id}` | `RoleAO` | 同上 |
| DELETE | `/api/admin/roles/{id}` | 无 | 同上（内置角色不可删） |
| GET | `/api/admin/roles/{id}/menus` | 无 | 同上 |
| PUT | `/api/admin/roles/{id}/menus` | `{ ids: [] }` 菜单 ID 列表 | 同上 |

> 系统菜单结构由 Flyway 迁移脚本维护，**不提供**「菜单权限管理」页面及 `/api/admin/menus` CRUD 接口。

---

## 三、WebSocket / STOMP 协议

### 3.1 连接

| 项 | 值 |
|----|-----|
| 端点 | `/ws`（SockJS） |
| 子协议 | STOMP |
| 认证 | `?token=<jwt>` |
| 心跳 | `[10000, 10000]` ms |
| 重连 | 客户端 `reconnectDelay=2000`，重连后自动恢复订阅 |

**前端实现**：`stores/ws.js` 全局单例，在 `Home.vue` 建立连接；`ChatRoom.vue` 订阅房间 Topic，`AuditPanel.vue` 订阅审核 Topic。

### 3.2 Broker 目的地

| 前缀 | 示例 | 方向 | 说明 |
|------|------|------|------|
| `/topic` | `/topic/room.123` | 下行广播 | 房间已通过消息 |
| `/topic` | `/topic/audit` | 下行广播 | 新待审核提醒（管理员） |
| `/user/queue` | `/user/queue/notifications` | 下行单播 | 个人通知（客户端订阅路径） |
| `/app` | `/app/*` | 上行 | 应用目的地前缀（本系统推送以服务端 `convertAndSend` 为主） |

### 3.3 订阅鉴权（服务端）

| 订阅目的地 | 条件 |
|------------|------|
| `/topic/room.{roomId}` | 当前用户对该 room 为 `JOINED` |
| `/topic/audit` | 拥有菜单路径 `/app/audit` |
| `/user/queue/notifications` | 握手 JWT 有效即可 |

### 3.4 消息信封 `WsMessage`

服务端推送 JSON 结构（`domain.dto.WsMessage`）：

```json
{
  "type": "CHAT",
  "roomId": 123,
  "messageId": 45678,
  "senderId": 1001,
  "senderName": "张三",
  "content": "文本内容",
  "status": "APPROVED",
  "timestamp": "2026-07-04T12:00:00+08:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 见下表 |
| roomId | Long | 聊天室 ID（部分通知可为 null） |
| messageId | Long | 消息 ID，**客户端去重键** |
| senderId | Long | 发送者用户 ID |
| senderName | String | 发送者昵称 |
| content | String | 正文 |
| status | String | 如 `APPROVED`、`REJECTED` |
| timestamp | ISO-8601 | 事件时间 |

#### type 枚举

| type | 方向 | 触发场景 | 订阅目的地 |
|------|------|----------|------------|
| `CHAT` | 下行 | 消息审核通过 | `/topic/room.{roomId}` |
| `NOTIFICATION` | 下行 | 审核拒绝、超时、加入审批结果 | `/user/queue/notifications` |
| `SYSTEM` | 下行 | 管理员广播/系统通知（绕过审核） | `/topic/room.{roomId}` |
| `HEARTBEAT` | 双向 | STOMP 协议层心跳 | — |
| `ACK` | 上行 | 客户端确认（预留） | — |

### 3.5 推送时机

| 事件 | 推送方式 |
|------|----------|
| 用户提交消息 | `pushPendingToAdmins` → `/topic/audit` |
| 审核通过 | `pushToRoom` → `/topic/room.{roomId}`，`type=CHAT` |
| 审核拒绝 / 超时 | `notifyUser` → `/user/queue/notifications`，`type=NOTIFICATION` |
| 管理员广播 | `pushToRoom`，`type=NOTIFICATION` 或 `SYSTEM` |

推送在 DB 事务 **`afterCommit`** 后异步执行，带有限次重试（at-least-once）。

### 3.6 客户端可靠性

1. **去重**：维护 `seenMessageIds`，同一 `messageId` 只处理一次。
2. **重连**：STOMP 断线自动重连 → `resubscribeAll()` 恢复所有 Topic。
3. **补齐**：重连后调用 `GET /api/rooms/{roomId}/messages` 拉取历史，与推送互补。

---

## 四、典型调用时序

### 4.1 用户发言 → 审核 → 实时展示

```
Client                REST API              AuditService           PushService           WS Client
  | POST /messages       |                      |                      |                    |
  |--------------------->| 落库 PENDING         |                      |                    |
  |                      | 入 Redis 队列         |                      |                    |
  |                      | pushPendingToAdmins  |--------------------->| /topic/audit       |
  |                      |                      |                      |------------------->|
  |                      |                      | POST approve         |                    |
  |                      |                      | CAS 更新 APPROVED    |                    |
  |                      |                      | afterCommit          |                    |
  |                      |                      |--------------------->| /topic/room.{id}   |
  |                      |                      |                      |------------------->|
  |                      |                      |                      |     messages.push  |
```

### 4.2 登录 → 在线人数 + WebSocket

```
Client           AuthService        LoggedInRepository      Home/ws Store
  | POST /login       |                    |                      |
  |------------------>| markLoggedIn       |                      |
  |<-- token ---------|                    |                      |
  | enter /app        |                    |                      |
  |                   |                    |    ensureConnected   |
  |                   |                    |    refreshLoggedIn   |
  |                   |                    |<---------------------|
  | subscribe /user/queue/notifications   |                      |
```

---

## 五、错误码（节选）

业务异常统一由 `GlobalExceptionHandler` 包装为 `ResultData`，常见场景：

| 场景 | 典型 message |
|------|----------------|
| 未登录 | 请先登录 |
| Token 无效/过期 | 令牌无效 |
| 无菜单权限 | 无访问权限 |
| 非房间成员 | 非聊天室成员 |
| 消息已审核 | 消息已被处理 |
| 审核并发冲突 | 审核冲突，请刷新 |

完整枚举见 `ErrorCodeEnum`。

---

## 六、相关文档

- [架构设计优化说明](./架构设计优化说明.md)
- [数据库设计](./数据库设计.md)
- [技术方案](../技术方案.md)
