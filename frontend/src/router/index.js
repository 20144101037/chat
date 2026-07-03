import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/app',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'rooms', name: 'RoomList', component: () => import('../views/RoomList.vue') },
      { path: 'rooms/:id', name: 'ChatRoom', component: () => import('../views/ChatRoom.vue') },
      { path: 'my-messages', name: 'MyMessages', component: () => import('../views/MyMessages.vue') },
      { path: 'broadcast', name: 'Broadcast', component: () => import('../views/Broadcast.vue'), meta: { admin: true } },
      { path: 'audit', name: 'AuditPanel', component: () => import('../views/AuditPanel.vue'), meta: { admin: true } },
      { path: 'metrics', name: 'MetricsDashboard', component: () => import('../views/MetricsDashboard.vue'), meta: { admin: true } },
      { path: 'configs', name: 'SystemConfig', component: () => import('../views/SystemConfig.vue'), meta: { admin: true } },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next('/login');
    return;
  }
  if (to.meta.admin) {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const isAdmin = ['ROOM_ADMIN', 'SYS_ADMIN'].includes(user?.role);
    if (!isAdmin) {
      next('/app/rooms');
      return;
    }
  }
  next();
});

export default router;
