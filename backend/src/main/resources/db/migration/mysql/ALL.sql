-- =============================================================
-- 多聊天室群聊系统 全量初始化脚本 (MySQL 8.0+)
--
-- 使用说明：
-- 1. 在空库或重置后的 chat 库中执行本脚本（会 CREATE TABLE + INSERT 初始数据）
-- 2. **执行完毕后必须重启后端**，由 AdminInitializer 创建 admin 账号并绑定 SYS_ADMIN 角色
-- 3. 若侧边栏仍无菜单：清理 Redis 键 chat:perm:* 后重新登录（或使用修复后的后端会自动刷新）
-- 4. 生产环境推荐使用 Flyway 迁移（V1~V5），本脚本仅供本地手工初始化
-- =============================================================
create database if not exists chat  collate utf8mb4_general_ci ;

use chat;
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

CREATE TABLE IF NOT EXISTS system_config (
                                             id           BIGINT       NOT NULL AUTO_INCREMENT,
                                             config_key   VARCHAR(100) NOT NULL,
    config_value VARCHAR(1000) DEFAULT NULL,
    config_group VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    description  VARCHAR(255) DEFAULT NULL,
    editable     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 初始化默认配置项（示例，可在「全局配置」菜单中维护）
INSERT INTO system_config (config_key, config_value, config_group, description) VALUES
                                                                                    ('audit.max-wait-seconds', '30', 'AUDIT', '消息审核最大等待时间（秒），超时自动拒绝'),
                                                                                    ('audit.scan-interval-ms', '5000', 'AUDIT', '审核超时扫描任务执行间隔（毫秒）'),
                                                                                    ('message.max-length', '1000', 'MESSAGE', '单条消息最大字符数'),
                                                                                    ('room.default-max-users', '500', 'ROOM', '新建聊天室默认最大人数'),
                                                                                    ('push.retry-times', '1', 'PUSH', '消息推送失败重试次数')
    ON DUPLICATE KEY UPDATE config_key = config_key;

INSERT INTO system_config (config_key, config_value, config_group, description) VALUES
    ('jwt.expire-minutes', '120', 'AUTH', '登录令牌有效期（分钟），默认2小时')
    ON DUPLICATE KEY UPDATE config_key = config_key;

-- =============================================================
-- RBAC 权限模型 (MySQL 8.0+)
-- 用户 - 角色（多对多）、角色 - 菜单（多对多）
-- =============================================================

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
                                     id          BIGINT       NOT NULL AUTO_INCREMENT,
                                     role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(50)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    built_in    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 菜单表（parent_id=0 表示顶级；menu_type: DIR 目录 / MENU 菜单）
CREATE TABLE IF NOT EXISTS menus (
                                     id         BIGINT       NOT NULL AUTO_INCREMENT,
                                     parent_id  BIGINT       NOT NULL DEFAULT 0,
                                     menu_key   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    path       VARCHAR(200) DEFAULT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    menu_type  VARCHAR(20)  NOT NULL DEFAULT 'MENU',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_key (menu_key)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 用户-角色关联
CREATE TABLE IF NOT EXISTS user_roles (
                                          id         BIGINT      NOT NULL AUTO_INCREMENT,
                                          user_id    BIGINT      NOT NULL,
                                          role_id    BIGINT      NOT NULL,
                                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 角色-菜单关联
CREATE TABLE IF NOT EXISTS role_menus (
                                          id         BIGINT      NOT NULL AUTO_INCREMENT,
                                          role_id    BIGINT      NOT NULL,
                                          menu_id    BIGINT      NOT NULL,
                                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 默认角色
INSERT INTO roles (role_code, role_name, description, built_in) VALUES
                                                                    ('SYS_ADMIN', '系统管理员', '拥有全部菜单权限', 1),
                                                                    ('ROOM_ADMIN', '聊天室管理员', '管理聊天室与消息审核等业务', 1),
                                                                    ('USER', '普通用户', '基础聊天功能', 1)
    ON DUPLICATE KEY UPDATE role_code = role_code;

-- 顶级菜单（含工作台，sort=0 排在最前）
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type) VALUES
                                                                         (0, 'dashboard', '工作台', '/app/dashboard', 0, 'MENU'),
                                                                         (0, 'rooms', '聊天室列表', '/app/rooms', 1, 'MENU'),
                                                                         (0, 'my-messages', '我的消息', '/app/my-messages', 2, 'MENU'),
                                                                         (0, 'broadcast', '消息广播', '/app/broadcast', 3, 'MENU'),
                                                                         (0, 'audit', '消息审核', '/app/audit', 4, 'MENU'),
                                                                         (0, 'metrics', '性能监控', '/app/metrics', 5, 'MENU'),
                                                                         (0, 'configs', '全局配置', '/app/configs', 6, 'MENU'),
                                                                         (0, 'system', '系统管理', NULL, 7, 'DIR')
    ON DUPLICATE KEY UPDATE menu_key = menu_key;

-- 系统管理子菜单（子查询包一层派生表规避 MySQL 同表读写限制）
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type)
SELECT t.id, 'system:users', '用户管理', '/app/system/users', 1, 'MENU'
FROM (SELECT id FROM menus WHERE menu_key = 'system') t
    ON DUPLICATE KEY UPDATE menu_key = menu_key;
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type)
SELECT t.id, 'system:roles', '角色管理', '/app/system/roles', 2, 'MENU'
FROM (SELECT id FROM menus WHERE menu_key = 'system') t
    ON DUPLICATE KEY UPDATE menu_key = menu_key;

-- 角色-菜单授权
-- 系统管理员：全部菜单
INSERT INTO role_menus (role_id, menu_id)
SELECT (SELECT id FROM roles WHERE role_code = 'SYS_ADMIN'), m.id
FROM menus m
    ON DUPLICATE KEY UPDATE role_id = role_id;
-- 聊天室管理员：业务菜单
INSERT INTO role_menus (role_id, menu_id)
SELECT (SELECT id FROM roles WHERE role_code = 'ROOM_ADMIN'), m.id
FROM menus m
WHERE m.menu_key IN ('rooms', 'my-messages', 'broadcast', 'audit')
    ON DUPLICATE KEY UPDATE role_id = role_id;
-- 普通用户：基础菜单
INSERT INTO role_menus (role_id, menu_id)
SELECT (SELECT id FROM roles WHERE role_code = 'USER'), m.id
FROM menus m
WHERE m.menu_key IN ('rooms', 'my-messages')
    ON DUPLICATE KEY UPDATE role_id = role_id;

-- 工作台对所有角色可见
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r, menus m
WHERE m.menu_key = 'dashboard'
    ON DUPLICATE KEY UPDATE role_id = role_id;