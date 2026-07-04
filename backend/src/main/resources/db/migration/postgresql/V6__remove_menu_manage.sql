-- =============================================================
-- 移除「菜单权限管理」菜单项（改由 Flyway 脚本维护菜单，不再提供 UI 管理）
-- =============================================================

DELETE FROM role_menus
WHERE menu_id IN (SELECT id FROM menus WHERE menu_key = 'system:menus');

DELETE FROM menus WHERE menu_key = 'system:menus';
