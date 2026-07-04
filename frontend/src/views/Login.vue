<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-top">
        <div class="brand-logo"><el-icon><ChatDotRound /></el-icon></div>
        <span class="brand-name">群聊管理平台</span>
      </div>
      <div class="brand-hero">
        <h1>多聊天室实时协作<br />与内容审核一站式管理</h1>
        <p>企业级群聊解决方案，支持多聊天室、消息审核、权限管理与运行监控。</p>
        <ul class="feature-list">
          <li><el-icon><ChatDotRound /></el-icon> 多聊天室实时消息推送</li>
          <li><el-icon><DocumentChecked /></el-icon> 消息先审后发，内容可控</li>
          <li><el-icon><UserFilled /></el-icon> RBAC 角色权限精细管理</li>
          <li><el-icon><DataLine /></el-icon> 在线用户与延迟实时监控</li>
        </ul>
      </div>
      <div class="brand-foot">© {{ year }} Chat Platform · Powered by Spring Boot & Vue 3</div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="form-card">
        <div class="form-head">
          <h2>{{ tab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ tab === 'login' ? '请登录你的账号以继续' : '注册一个新账号开始体验' }}</p>
        </div>

        <el-tabs v-model="tab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form :model="form" size="large" @submit.prevent>
              <el-form-item>
                <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="onLogin" />
              </el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loading" @click="onLogin">登 录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form :model="form" size="large" @submit.prevent>
              <el-form-item>
                <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="Avatar" clearable />
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="onRegister" />
              </el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loading" @click="onRegister">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Avatar } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
import { authApi } from '../api';

const router = useRouter();
const auth = useAuthStore();
const tab = ref('login');
const loading = ref(false);
const form = reactive({ username: '', password: '', nickname: '' });
const year = computed(() => new Date().getFullYear());

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名与密码');
    return;
  }
  loading.value = true;
  try {
    await auth.login({ username: form.username, password: form.password });
    router.push('/app/dashboard');
  } finally {
    loading.value = false;
  }
}

async function onRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名与密码');
    return;
  }
  loading.value = true;
  try {
    await authApi.register({ username: form.username, password: form.password, nickname: form.nickname });
    ElMessage.success('注册成功，请登录');
    tab.value = 'login';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 左侧品牌 */
.brand-side {
  flex: 1.1;
  background: linear-gradient(150deg, #1b2440 0%, #3b3f8f 55%, #6b4fd8 100%);
  color: #fff;
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}
.brand-side::after {
  content: '';
  position: absolute;
  width: 460px; height: 460px;
  right: -140px; top: -120px;
  background: radial-gradient(circle, rgba(255,255,255,0.14) 0%, transparent 70%);
  border-radius: 50%;
}
.brand-top { display: flex; align-items: center; gap: 12px; z-index: 1; }
.brand-logo {
  width: 44px; height: 44px; border-radius: 12px;
  background: rgba(255,255,255,0.16);
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.brand-name { font-size: 20px; font-weight: 700; }
.brand-hero { z-index: 1; }
.brand-hero h1 { font-size: 34px; line-height: 1.35; margin: 0 0 18px; font-weight: 700; }
.brand-hero p { font-size: 15px; opacity: 0.85; max-width: 460px; line-height: 1.7; }
.feature-list { list-style: none; padding: 0; margin: 30px 0 0; }
.feature-list li {
  display: flex; align-items: center; gap: 12px;
  font-size: 15px; margin-bottom: 16px; opacity: 0.95;
}
.feature-list .el-icon {
  background: rgba(255,255,255,0.16); border-radius: 8px;
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
}
.brand-foot { font-size: 12px; opacity: 0.6; z-index: 1; }

/* 右侧表单 */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
  padding: 24px;
}
.form-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 18px;
  padding: 40px 36px;
  box-shadow: var(--shadow-card);
}
.form-head { margin-bottom: 12px; }
.form-head h2 { margin: 0 0 6px; font-size: 24px; font-weight: 700; color: var(--text-main); }
.form-head p { margin: 0; font-size: 14px; color: var(--text-secondary); }
.submit-btn { width: 100%; height: 44px; font-size: 16px; letter-spacing: 4px; margin-top: 6px; }

@media (max-width: 860px) {
  .brand-side { display: none; }
}
</style>
