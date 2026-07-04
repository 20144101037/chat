<template>
  <div class="page">
    <PageHeader title="用户管理" subtitle="管理用户账号、分配角色与账号状态" icon="User">
      <template #actions>
        <el-input v-model="query.keyword" placeholder="搜索用户名/昵称" style="width:200px" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px" @change="load">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="封禁" value="BANNED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
    <el-table :data="records" v-loading="loading" style="width:100%" empty-text="暂无用户">
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="name in row.roleNames" :key="name" style="margin-right:6px">{{ name }}</el-tag>
          <span v-if="!row.roleNames || !row.roleNames.length" style="color:#c0c4cc">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="主角色" width="120">
        <template #default="{ row }">{{ row.role }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ row.status === 'ACTIVE' ? '正常' : '封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Avatar" @click="openRoles(row)">分配角色</el-button>
          <el-button type="warning" link :icon="Key" @click="openReset(row)">重置密码</el-button>
          <el-button v-if="row.status === 'ACTIVE'" type="danger" link :icon="CircleClose" @click="toggleStatus(row, 'BANNED')">封禁</el-button>
          <el-button v-else type="success" link :icon="CircleCheck" @click="toggleStatus(row, 'ACTIVE')">启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="prev, pager, next, total, sizes"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      :page-sizes="[10, 20, 50]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
    </el-card>

    <el-dialog v-model="dialog" :title="`分配角色 - ${current?.username || ''}`" width="420px">
      <el-checkbox-group v-model="selectedRoleIds">
        <div v-for="r in allRoles" :key="r.id" class="role-item">
          <el-checkbox :label="r.id">{{ r.roleName }}（{{ r.roleCode }}）</el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialog" :title="`重置密码 - ${current?.username || ''}`" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" @click="saveReset">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Avatar, CircleClose, CircleCheck, Key } from '@element-plus/icons-vue';
import { adminApi } from '../../api';
import PageHeader from '../../components/PageHeader.vue';

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({ keyword: '', status: '', pageNo: 1, pageSize: 10 });

const allRoles = ref([]);
const dialog = ref(false);
const current = ref(null);
const selectedRoleIds = ref([]);

const pwdDialog = ref(false);
const pwdFormRef = ref(null);
const pwdForm = reactive({ password: '', confirm: '' });
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 之间', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) => (value === pwdForm.password ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur',
    },
  ],
};

async function load() {
  loading.value = true;
  try {
    const res = await adminApi.userPage(query);
    records.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function onPageChange(p) { query.pageNo = p; load(); }
function onSizeChange(s) { query.pageSize = s; query.pageNo = 1; load(); }

async function openRoles(row) {
  current.value = row;
  if (!allRoles.value.length) {
    allRoles.value = await adminApi.roleAll();
  }
  selectedRoleIds.value = await adminApi.userRoles(row.id);
  dialog.value = true;
}

async function saveRoles() {
  await adminApi.assignUserRoles(current.value.id, selectedRoleIds.value);
  ElMessage.success('角色已更新');
  dialog.value = false;
  load();
}

async function toggleStatus(row, status) {
  await adminApi.updateUserStatus(row.id, status);
  ElMessage.success('状态已更新');
  load();
}

function openReset(row) {
  current.value = row;
  pwdForm.password = '';
  pwdForm.confirm = '';
  pwdDialog.value = true;
}

async function saveReset() {
  await pwdFormRef.value.validate();
  await adminApi.resetUserPassword(current.value.id, pwdForm.password);
  ElMessage.success('密码已重置，该用户旧登录状态已失效');
  pwdDialog.value = false;
}

onMounted(load);
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.role-item { margin-bottom: 8px; }
</style>
