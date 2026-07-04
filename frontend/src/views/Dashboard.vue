<template>
  <div class="page dashboard">
    <!-- 欢迎横幅 -->
    <div class="hero surface-card">
      <div class="hero-info">
        <div class="hero-greeting">{{ greeting }}，{{ auth.user?.nickname || auth.user?.username }} 👋</div>
        <div class="hero-sub">
          <el-tag effect="dark" round>{{ roleText }}</el-tag>
          <span class="hero-date">{{ today }}</span>
        </div>
        <p class="hero-tip">欢迎回到多聊天室群聊管理平台，祝你使用愉快。</p>
      </div>
      <div class="hero-illust"><el-icon><ChatDotRound /></el-icon></div>
    </div>

    <!-- 指标卡 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="s in stats" :key="s.label" :xs="12" :sm="12" :md="6">
        <div class="stat-card surface-card">
          <div class="stat-icon" :style="{ background: s.bg }"><el-icon><component :is="s.icon" /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 快捷入口 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header><span class="card-title"><el-icon><Grid /></el-icon> 快捷入口</span></template>
          <div class="quick-grid">
            <div v-for="q in quickEntries" :key="q.path" class="quick-item" @click="go(q.path)">
              <div class="quick-icon"><el-icon><component :is="q.icon" /></el-icon></div>
              <span>{{ q.name }}</span>
            </div>
            <el-empty v-if="!quickEntries.length" description="暂无可用功能" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <!-- 我的聊天室 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="rooms-card">
          <template #header>
            <span class="card-title"><el-icon><ChatDotRound /></el-icon> 我的聊天室</span>
          </template>
          <el-scrollbar height="260px">
            <div v-for="r in myRooms" :key="r.id" class="room-line" @click="go(`/app/rooms/${r.id}`)">
              <el-avatar :size="34" class="room-avatar">{{ (r.name || '?').charAt(0) }}</el-avatar>
              <div class="room-meta">
                <div class="room-name">{{ r.name }}</div>
                <div class="room-desc">{{ r.description || '暂无简介' }}</div>
              </div>
              <el-icon class="room-arrow"><ArrowRight /></el-icon>
            </div>
            <el-empty v-if="!myRooms.length" description="还没有加入聊天室" :image-size="80" />
          </el-scrollbar>
          <el-button text type="primary" class="rooms-more" @click="go('/app/rooms')">
            浏览全部聊天室 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { roomApi, messageApi, auditApi, metricsApi } from '../api';
import { iconOf } from '../constants/menuIcons';

const router = useRouter();
const auth = useAuthStore();

const myRooms = ref([]);
const counters = reactive({ totalRooms: 0, myRooms: 0, myPending: 0, approved: 0, pendingAudit: 0, online: 0 });
let onlineTimer = null;

const roleTextMap = { SYS_ADMIN: '系统管理员', ROOM_ADMIN: '聊天室管理员', USER: '普通用户' };
const roleText = computed(() => roleTextMap[auth.user?.role] || auth.user?.role || '成员');

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return '夜深了';
  if (h < 12) return '早上好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
});

const today = computed(() => {
  const d = new Date();
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${week}`;
});

const stats = computed(() => {
  const base = [
    { label: '我的聊天室', value: counters.myRooms, icon: 'ChatDotRound', bg: 'linear-gradient(135deg,#5b7cfa,#7c5cff)' },
    { label: '我的待审核', value: counters.myPending, icon: 'AlarmClock', bg: 'linear-gradient(135deg,#ffa53e,#ff7a45)' },
    { label: '我的已通过', value: counters.approved, icon: 'CircleCheck', bg: 'linear-gradient(135deg,#3fce7a,#12b76a)' },
  ];
  if (auth.hasMenuPath('/app/metrics')) {
    base.push({ label: '在线人数', value: counters.online, icon: 'User', bg: 'linear-gradient(135deg,#38bdf8,#0ea5e9)' });
  } else {
    base.push({ label: '聊天室总数', value: counters.totalRooms, icon: 'Files', bg: 'linear-gradient(135deg,#38bdf8,#0ea5e9)' });
  }
  return base;
});

const quickEntries = computed(() => {
  const flat = [];
  (auth.menus || []).forEach((m) => {
    if (m.menuKey === 'dashboard') return;
    if (m.children && m.children.length) {
      m.children.forEach((c) => flat.push(c));
    } else if (m.path) {
      flat.push(m);
    }
  });
  return flat.map((m) => ({ name: m.name, path: m.path, icon: iconOf(m.menuKey) }));
});

function go(path) {
  if (path) router.push(path);
}

async function loadCounters() {
  const tasks = [
    roomApi.myRooms().then((list) => {
      myRooms.value = list || [];
      counters.myRooms = myRooms.value.length;
    }),
    roomApi.page({ pageNo: 1, pageSize: 1 }).then((res) => { counters.totalRooms = res.total; }),
    messageApi.myMessages({ pageNo: 1, pageSize: 1, status: 'PENDING_REVIEW' }).then((res) => { counters.myPending = res.total; }),
    messageApi.myMessages({ pageNo: 1, pageSize: 1, status: 'APPROVED' }).then((res) => { counters.approved = res.total; }),
  ];
  if (auth.hasMenuPath('/app/metrics')) {
    tasks.push(metricsApi.dashboard().then((res) => { counters.online = res.onlineUsers; }));
  }
  if (auth.hasMenuPath('/app/audit')) {
    tasks.push(auditApi.pending({ pageNo: 1, pageSize: 1 }).then((res) => { counters.pendingAudit = res.total; }));
  }
  await Promise.allSettled(tasks);
}

onMounted(async () => {
  await auth.loadMenus();
  await loadCounters();
  if (auth.hasMenuPath('/app/metrics')) {
    onlineTimer = setInterval(async () => {
      try {
        const res = await metricsApi.dashboard();
        counters.online = res.onlineUsers;
      } catch (e) {
        // 忽略轮询失败
      }
    }, 15000);
  }
});

onUnmounted(() => {
  if (onlineTimer) clearInterval(onlineTimer);
});
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 28px;
  background: var(--brand-gradient);
  color: #fff;
  border: none;
  overflow: hidden;
  position: relative;
}
.hero-greeting { font-size: 24px; font-weight: 700; }
.hero-sub { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
.hero-sub :deep(.el-tag) { background: rgba(255, 255, 255, 0.22); border: none; color: #fff; }
.hero-date { font-size: 13px; opacity: 0.85; }
.hero-tip { margin: 12px 0 0; font-size: 13px; opacity: 0.9; }
.hero-illust { font-size: 96px; opacity: 0.22; margin-right: 12px; }

.stat-row { margin: 0; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  margin-bottom: 16px;
}
.stat-icon {
  width: 50px; height: 50px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 24px; flex-shrink: 0;
}
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.1; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.card-title { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; }

.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.quick-item {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 18px 10px; border-radius: 12px; cursor: pointer;
  border: 1px solid var(--border-soft); transition: all 0.2s ease;
  font-size: 13px; color: var(--text-regular);
}
.quick-item:hover { transform: translateY(-3px); border-color: var(--brand-1); color: var(--brand-1); box-shadow: var(--shadow-hover); }
.quick-icon {
  width: 42px; height: 42px; border-radius: 12px; font-size: 20px;
  display: flex; align-items: center; justify-content: center;
  background: var(--el-color-primary-light-9); color: var(--brand-1);
}
.quick-item:hover .quick-icon { background: var(--brand-gradient); color: #fff; }

.rooms-card :deep(.el-card__body) { padding-bottom: 8px; }
.room-line {
  display: flex; align-items: center; gap: 12px; padding: 10px 6px;
  border-radius: 10px; cursor: pointer; transition: background 0.2s;
}
.room-line:hover { background: var(--el-color-primary-light-9); }
.room-avatar { background: var(--brand-gradient); color: #fff; font-weight: 600; flex-shrink: 0; }
.room-meta { flex: 1; min-width: 0; }
.room-name { font-weight: 600; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-desc { font-size: 12px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-arrow { color: var(--text-secondary); }
.rooms-more { width: 100%; margin-top: 6px; }

@media (max-width: 768px) {
  .quick-grid { grid-template-columns: repeat(3, 1fr); }
  .hero-illust { display: none; }
}
</style>
