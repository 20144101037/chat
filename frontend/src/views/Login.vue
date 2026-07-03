<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 style="text-align:center">多聊天室群聊系统</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" label-width="70px">
            <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
            <el-button type="primary" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="form" label-width="70px">
            <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
            <el-button type="primary" style="width:100%" :loading="loading" @click="onRegister">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <p style="color:#909399;font-size:12px;text-align:center">默认管理员 admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../stores/auth';
import { authApi } from '../api';

const router = useRouter();
const auth = useAuthStore();
const tab = ref('login');
const loading = ref(false);
const form = reactive({ username: '', password: '', nickname: '' });

async function onLogin() {
  loading.value = true;
  try {
    await auth.login({ username: form.username, password: form.password });
    router.push('/app/rooms');
  } finally {
    loading.value = false;
  }
}

async function onRegister() {
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
.login-wrap { display: flex; align-items: center; justify-content: center; height: 100vh; }
.login-card { width: 400px; }
</style>
