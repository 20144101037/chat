# 接口与 WebSocket 协议说明

> 统一响应体：`{ "code": 0, "message": "ok", "data": {...}, "traceId": "...", "timestamp": 0 }`；`code=0` 为成功。
> 认证：除登录/注册外，REST 请求需携带 `Authorization: Bearer <token>`。

## 一、REST API

### 认证
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 | 公开 |
| POST | `/api/auth/login` | 登录，返回 token + 用户信息 | 公开 |

### 聊天室
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/rooms` | 创建聊天室 | 管理员 |
| PUT | `/api/rooms/{id}` | 修改聊天室 | 管理员 |
| DELETE | `/api/rooms/{id}` | 删除（逻辑） | 管理员 |
| PATCH | `/api/rooms/{id}/status` | 变更状态 `{status}` | 管理员 |
| GET | `/api/rooms?keyword=&status=&pageNo=&pageSize=` | 分页列表 | 登录 |
| GET | `/api/rooms/{id}` | 详情 | 登录 |
| POST | `/api/rooms/broadcast` | 多房间广播 `{roomIds,content}` | 管理员 |
| POST | `/api/rooms/{id}/system-notify` | 紧急/系统通知 `{content}` | 管理员 |

### 成员
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/rooms/{id}/join` | 申请/加入，返回 `{memberStatus}` | 登录 |
| POST | `/api/rooms/{id}/leave` | 退出 | 登录 |
| POST | `/api/rooms/{id}/members/{userId}/approve` | 审批加入 `{pass}` | 管理员 |
| GET | `/api/rooms/mine` | 我加入的聊天室 | 登录 |

### 消息
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/rooms/{roomId}/messages` | 提交消息（进审核）`{content}` | 成员 |
| GET | `/api/rooms/{roomId}/messages?before=&size=` | 已通过历史（游标分页） | 成员 |
| GET | `/api/me/messages?status=` | 我的消息与审核状态 | 登录 |

### 审核
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/audit/pending?roomId=&pageNo=&pageSize=` | 待审核列表 | 管理员 |
| POST | `/api/audit/{messageId}/approve` | 通过 | 管理员 |
| POST | `/api/audit/{messageId}/reject` | 拒绝 `{reason}` | 管理员 |
| POST | `/api/audit/batch` | 批量 `{messageIds,action,reason}` | 管理员 |

## 二、WebSocket / STOMP 协议

- 端点：`/ws`（SockJS），握手参数 `?token=<jwt>` 完成认证。
- 心跳：`heartbeat[10000,10000]`。
- 目的地：
  - 订阅房间消息：`/topic/room.{roomId}`（需为已加入成员）
  - 个人通知：`/user/queue/notifications`
  - 管理员待审核：`/topic/audit`（需管理员）

### 消息信封（JSON）

```json
{
  "type": "CHAT | NOTIFICATION | SYSTEM | HEARTBEAT | ACK",
  "roomId": 123,
  "messageId": 45678,
  "senderId": 1001,
  "senderName": "张三",
  "content": "文本内容",
  "status": "APPROVED",
  "timestamp": "2026-07-03T19:00:00+08:00"
}
```

| type | 方向 | 说明 |
|------|------|------|
| CHAT | 下行 | 已审核通过的聊天消息 |
| NOTIFICATION | 下行 | 审核结果、超时、加入审批通知 |
| SYSTEM | 下行 | 管理员系统通知/紧急消息 |
| HEARTBEAT | 双向 | STOMP 内置心跳 |
| ACK | 上行 | 客户端确认（配合去重） |

### 可靠性
- 推送为 at-least-once；客户端按 `messageId` 去重。
- 断线自动重连，重连后重新订阅并可用历史接口补齐消息。

## 三、线上问题监控与处理流程

1. **指标**：`/actuator/prometheus` 暴露在线用户数（`chat_online_users`）、待审核队列长度（`chat_audit_pending_size`）、接口延迟、连接池使用率等。
2. **报警**：Prometheus Alertmanager 规则——队列积压、P99 延迟、错误率、连接池耗尽触发告警。
3. **排查**：结构化日志 + TraceId 串联链路；Grafana 大盘观测趋势。
4. **回滚**：制品版本化，Flyway 迁移可回滚，灰度发布，异常一键回滚。
