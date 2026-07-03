-- =============================================================
-- 全局配置表 (MySQL 8.0+)
-- 将分散在 application.yml 的业务参数收敛到 DB，便于运行期动态调整与功能扩展
-- =============================================================
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
