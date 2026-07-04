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

-- 顶级菜单
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type) VALUES
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
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type)
SELECT t.id, 'system:menus', '菜单权限管理', '/app/system/menus', 3, 'MENU'
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
