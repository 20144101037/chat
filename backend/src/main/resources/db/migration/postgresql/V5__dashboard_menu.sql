-- =============================================================
-- 新增「工作台」概览菜单（PostgreSQL），作为登录后的默认落地页
-- =============================================================
INSERT INTO menus (parent_id, menu_key, name, path, sort, menu_type) VALUES
    (0, 'dashboard', '工作台', '/app/dashboard', 0, 'MENU')
    ON CONFLICT DO NOTHING;

-- 工作台对所有角色可见
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r CROSS JOIN menus m
WHERE m.menu_key = 'dashboard'
    ON CONFLICT DO NOTHING;
