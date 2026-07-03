<template>
  <el-container style="height:100vh">
    <el-aside width="200px" class="aside">
      <div class="brand">群聊系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/app/rooms">聊天室列表</el-menu-item>
        <el-menu-item index="/app/my-messages">我的消息</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/app/broadcast">消息广播</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/app/audit">消息审核</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/app/metrics">性能监控</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/app/configs">全局配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ auth.user?.nickname }}（{{ auth.user?.role }}）</span>
        <el-button link @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

function logout() {
  auth.logout();
  router.push('/login');
}
</script>

<style scoped>
.aside { background: #001529; color: #fff; }
.brand { height: 60px; line-height: 60px; text-align: center; color: #fff; font-size: 18px; font-weight: bold; }
.header { display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #eee; }
</style>
