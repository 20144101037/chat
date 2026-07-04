<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="brand" :class="{ mini: collapsed }">
        <div class="brand-logo"><el-icon><ChatDotRound /></el-icon></div>
        <span v-show="!collapsed" class="brand-text">群聊平台</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          background-color="transparent"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#ffffff"
        >
          <template v-for="menu in menus" :key="menu.menuKey">
            <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.menuKey">
              <template #title>
                <el-icon><component :is="iconOf(menu.menuKey)" /></el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.menuKey" :index="child.path">
                <el-icon><component :is="iconOf(child.menuKey)" /></el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menu.path">
              <el-icon><component :is="iconOf(menu.menuKey)" /></el-icon>
              <span>{{ menu.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb :separator-icon="ArrowRight">
            <el-breadcrumb-item>{{ topLevelTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle !== topLevelTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
                <span class="user-role">{{ roleText }}</span>
              </div>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="dashboard" :icon="Odometer">工作台</el-dropdown-item>
                <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="view-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, CaretBottom, Odometer, SwitchButton } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
import { useWsStore } from '../stores/ws';
import { iconOf } from '../constants/menuIcons';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const ws = useWsStore();
const collapsed = ref(false);

const menus = computed(() => auth.menus);

const roleTextMap = { SYS_ADMIN: '系统管理员', ROOM_ADMIN: '聊天室管理员', USER: '普通用户' };
const roleText = computed(() => roleTextMap[auth.user?.role] || auth.user?.role || '成员');
const avatarText = computed(() => {
  const n = auth.user?.nickname || auth.user?.username || '?';
  return n.charAt(0).toUpperCase();
});

const routeTitles = {
  '/app/dashboard': '工作台',
  '/app/rooms': '聊天室列表',
  '/app/my-messages': '我的消息',
  '/app/broadcast': '消息广播',
  '/app/audit': '消息审核',
  '/app/metrics': '性能监控',
  '/app/configs': '全局配置',
  '/app/system/users': '用户管理',
  '/app/system/roles': '角色管理',
  '/app/system/menus': '菜单权限管理',
};

const currentTitle = computed(() => {
  if (route.path.startsWith('/app/rooms/')) return '聊天室';
  return routeTitles[route.path] || '';
});

const topLevelTitle = computed(() => {
  if (route.path.startsWith('/app/system')) return '系统管理';
  return currentTitle.value || '工作台';
});

const activeMenu = computed(() => {
  if (route.path.startsWith('/app/rooms/')) return '/app/rooms';
  return route.path;
});

function onCommand(cmd) {
  if (cmd === 'logout') {
    auth.logout().then(() => router.push('/login'));
  } else if (cmd === 'dashboard') {
    router.push('/app/dashboard');
  }
}

onMounted(async () => {
  try {
    await auth.loadMenus();
  } catch (e) {
    // 菜单加载失败时保持空侧边栏，错误已由拦截器提示
  }
  try {
    await ws.ensureConnected();
    ws.subscribeNotifications((n) => {
      if (n?.content) ElMessage.info(n.content);
    });
  } catch (e) {
    // WebSocket 连接失败时不阻断主界面
  }
});
</script>

<style scoped>
.layout { height: 100vh; }

.aside {
  background: var(--sidebar-gradient);
  transition: width 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand.mini { justify-content: center; padding: 0; }
.brand-logo {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  background: var(--brand-gradient);
  display: flex; align-items: center; justify-content: center; font-size: 19px;
}
.brand-text { font-size: 17px; font-weight: 700; white-space: nowrap; }
.menu-scroll { flex: 1; }

.aside :deep(.el-menu-item),
.aside :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  margin: 4px 10px;
  border-radius: 8px;
}
.aside :deep(.el-menu-item.is-active) {
  background: var(--brand-gradient) !important;
  box-shadow: 0 4px 12px rgba(91, 124, 250, 0.4);
}
.aside :deep(.el-menu-item:hover),
.aside :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}
.aside :deep(.el-sub-menu .el-menu-item) { min-width: 0; }

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--border-soft);
  padding: 0 22px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: var(--text-regular); }
.collapse-btn:hover { color: var(--brand-1); }

.user-chip {
  display: flex; align-items: center; gap: 10px;
  padding: 5px 10px; border-radius: 10px; cursor: pointer;
  transition: background 0.2s;
}
.user-chip:hover { background: var(--bg-page); }
.user-avatar { background: var(--brand-gradient); color: #fff; font-weight: 600; }
.user-meta { display: flex; flex-direction: column; line-height: 1.2; }
.user-name { font-size: 14px; font-weight: 600; color: var(--text-main); }
.user-role { font-size: 12px; color: var(--text-secondary); }

.main { background: var(--bg-page); padding: 20px; }

.view-fade-enter-active, .view-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.view-fade-enter-from { opacity: 0; transform: translateY(8px); }
.view-fade-leave-to { opacity: 0; }
</style>
