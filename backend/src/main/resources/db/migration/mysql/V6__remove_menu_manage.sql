-- =============================================================
-- 移除「菜单权限管理」菜单项（改由 Flyway 脚本维护菜单，不再提供 UI 管理）
-- =============================================================

DELETE rm FROM role_menus rm
INNER JOIN menus m ON rm.menu_id = m.id
WHERE m.menu_key = 'system:menus';

DELETE FROM menus WHERE menu_key = 'system:menus';
