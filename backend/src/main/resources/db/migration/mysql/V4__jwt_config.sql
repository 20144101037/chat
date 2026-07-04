-- =============================================================
-- 将登录令牌有效期收敛到全局配置（MySQL）
-- 默认 120 分钟（2 小时）
-- =============================================================
INSERT INTO system_config (config_key, config_value, config_group, description) VALUES
    ('jwt.expire-minutes', '120', 'AUTH', '登录令牌有效期（分钟），默认2小时')
    ON DUPLICATE KEY UPDATE config_key = config_key;
