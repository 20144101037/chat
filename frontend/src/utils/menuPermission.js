/** 从菜单树收集全部可访问路径 */
export function collectMenuPaths(menus) {
  const paths = new Set();
  const walk = (items) => {
    for (const m of items || []) {
      if (m.path) paths.add(m.path);
      if (m.children?.length) walk(m.children);
    }
  };
  walk(menus);
  return paths;
}

/** 判断目标路由是否允许访问 */
export function canAccessRoute(path, menuPaths) {
  if (path === '/app' || path === '/app/dashboard') return true;
  if (/^\/app\/rooms\/\d+$/.test(path)) return menuPaths.has('/app/rooms');
  return menuPaths.has(path);
}
