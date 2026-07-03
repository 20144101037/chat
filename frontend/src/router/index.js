import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { canAccessRoute } from '../utils/menuPermission';

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/app',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true },
    redirect: '/app/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'rooms', name: 'RoomList', component: () => import('../views/RoomList.vue') },
      { path: 'rooms/:id', name: 'ChatRoom', component: () => import('../views/ChatRoom.vue') },
      { path: 'my-messages', name: 'MyMessages', component: () => import('../views/MyMessages.vue') },
      { path: 'broadcast', name: 'Broadcast', component: () => import('../views/Broadcast.vue') },
      { path: 'audit', name: 'AuditPanel', component: () => import('../views/AuditPanel.vue') },
      { path: 'metrics', name: 'MetricsDashboard', component: () => import('../views/MetricsDashboard.vue') },
      { path: 'configs', name: 'SystemConfig', component: () => import('../views/SystemConfig.vue') },
      { path: 'system/users', name: 'UserManage', component: () => import('../views/system/UserManage.vue') },
      { path: 'system/roles', name: 'RoleManage', component: () => import('../views/system/RoleManage.vue') },
      { path: 'system/menus', name: 'MenuManage', component: () => import('../views/system/MenuManage.vue') },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next('/login');
    return;
  }
  if (to.path.startsWith('/app') && token) {
    const auth = useAuthStore();
    if (!auth.menusLoaded) {
      try {
        await auth.loadMenus();
      } catch {
        next('/login');
        return;
      }
    }
    if (!canAccessRoute(to.path, auth.menuPaths)) {
      next('/app/dashboard');
      return;
    }
  }
  next();
});

export default router;
