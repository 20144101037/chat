-- =============================================================
-- 多聊天室群聊系统 初始化脚本 (MySQL 8.0+)
-- 说明：MySQL 不支持部分索引(WHERE 条件索引)，唯一索引改为普通唯一键；
--       时间列用 DATETIME(6)，与实体 OffsetDateTime 通过 Connector/J 转换。
-- =============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50)  DEFAULT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 聊天室表
CREATE TABLE IF NOT EXISTS chat_rooms (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    max_users   INT          NOT NULL DEFAULT 500,
    join_policy VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    owner_id    BIGINT       DEFAULT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rooms_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    room_id      BIGINT       NOT NULL,
    sender_id    BIGINT       NOT NULL,
    content      TEXT         NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'CHAT',
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW',
    submitted_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at  DATETIME(6)  DEFAULT NULL,
    reviewer_id  BIGINT       DEFAULT NULL,
    version      INT          NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_messages_room_status_time (room_id, status, submitted_at),
    KEY idx_messages_sender (sender_id),
    KEY idx_messages_status_time (status, submitted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 审核日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    message_id  BIGINT       NOT NULL,
    reviewer_id BIGINT       DEFAULT NULL,
    action      VARCHAR(20)  NOT NULL,
    reason      VARCHAR(500) DEFAULT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_audit_message (message_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 用户-聊天室关联表
CREATE TABLE IF NOT EXISTS user_chat_rooms (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    room_id       BIGINT      NOT NULL,
    member_status VARCHAR(20) NOT NULL DEFAULT 'JOINED',
    role_in_room  VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at     DATETIME(6) DEFAULT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted       TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_room (user_id, room_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 说明：系统管理员账号（admin / admin123）由应用启动时的 AdminInitializer 使用
-- 真实 PasswordEncoder 加密后写入，避免脚本硬编码哈希不一致，见 com.jin.chat.config.AdminInitializer
