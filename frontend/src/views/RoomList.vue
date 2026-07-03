<template>
  <div class="page">
    <PageHeader title="聊天室列表" subtitle="浏览、加入并管理聊天室" icon="ChatDotRound">
      <template #actions>
        <el-input v-model="query.keyword" placeholder="搜索聊天室名称" style="width:200px" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px" @change="load">
          <el-option label="活跃" value="ACTIVE" />
          <el-option label="暂停" value="PAUSED" />
          <el-option label="关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button v-if="auth.isAdmin" type="success" :icon="Plus" @click="openCreate">创建聊天室</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
    <el-table :data="page.records" style="width:100%" v-loading="loading" empty-text="暂无聊天室">
      <el-table-column label="名称" min-width="160">
        <template #default="{ row }">
          <div class="room-cell">
            <el-avatar :size="30" class="room-av">{{ (row.name || '?').charAt(0) }}</el-avatar>
            <span class="room-nm">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="加入策略" width="100">
        <template #default="{ row }">{{ row.joinPolicy === 'OPEN' ? '开放' : '需审批' }}</template>
      </el-table-column>
      <el-table-column label="成员" width="100">
        <template #default="{ row }">{{ row.memberCount }}/{{ row.maxUsers }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="360">
        <template #default="{ row }">
          <el-button v-if="row.myMemberStatus === 'JOINED'" type="primary" link @click="enter(row)">进入</el-button>
          <el-button v-if="row.myMemberStatus === 'JOINED'" type="warning" link @click="leave(row)">退出</el-button>
          <el-button v-else-if="row.myMemberStatus === 'PENDING'" type="info" link disabled>待审批</el-button>
          <el-button v-else type="success" link @click="join(row)">加入</el-button>

          <template v-if="auth.isAdmin">
            <el-divider direction="vertical" />
            <el-button type="primary" link @click="openEdit(row)">修改</el-button>
            <el-button type="primary" link :icon="User" @click="openMemberManage(row)">人员管理</el-button>
            <el-button type="info" link @click="openNotify(row)">通知</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="page.total > query.pageSize"
      style="margin-top:16px;justify-content:flex-end"
      layout="prev, pager, next, total"
      :total="page.total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      @current-change="onPageChange"
    />
    </el-card>

    <!-- 创建/修改 弹框 -->
    <el-dialog v-model="dialog" :title="editingId ? '修改聊天室' : '创建聊天室'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="最大人数"><el-input-number v-model="form.maxUsers" :min="1" /></el-form-item>
        <el-form-item label="加入策略">
          <el-select v-model="form.joinPolicy">
            <el-option label="开放（申请即加入）" value="OPEN" />
            <el-option label="需审批" value="APPROVAL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingId" label="聊天室状态">
          <el-select v-model="form.status">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="暂停" value="PAUSED" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 人员管理 弹框 -->
    <el-dialog v-model="manageDialog" :title="`人员管理 - ${currentRoom?.name || ''}`" width="640px">
      <div class="manage-section">
        <div class="section-title">拉人进群</div>
        <div class="add-row">
          <el-select
            v-model="selectedUserId"
            filterable
            remote
            clearable
            placeholder="搜索用户名或昵称"
            :remote-method="searchCandidates"
            :loading="candidateLoading"
            style="flex:1"
          >
            <el-option
              v-for="u in candidates"
              :key="u.userId"
              :label="`${u.nickname || u.username} (${u.username})`"
              :value="u.userId"
            />
          </el-select>
          <el-button type="primary" :icon="Plus" :disabled="!selectedUserId" @click="addMember">拉入</el-button>
        </div>
      </div>

      <div class="manage-section">
        <div class="section-title">当前成员</div>
        <el-table :data="members" v-loading="memberLoading" empty-text="暂无成员" size="small">
          <el-table-column label="用户" min-width="140">
            <template #default="{ row }">{{ row.nickname || row.username }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="memberStatusTag(row.memberStatus)" size="small">
                {{ memberStatusText(row.memberStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="160">
            <template #default="{ row }">{{ row.joinedAt ? formatTime(row.joinedAt) : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <template v-if="row.memberStatus === 'PENDING'">
                <el-button type="success" link size="small" @click="approve(row, true)">通过</el-button>
                <el-button type="danger" link size="small" @click="approve(row, false)">拒绝</el-button>
              </template>
              <el-button
                v-else-if="row.memberStatus === 'JOINED' && row.userId !== currentRoom?.ownerId"
                type="danger"
                link
                size="small"
                @click="kickMember(row)"
              >移出</el-button>
              <span v-else-if="row.memberStatus === 'JOINED'" class="muted-tip">创建者</span>
              <span v-else class="muted-tip">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 系统通知 弹框 -->
    <el-dialog v-model="notifyDialog" :title="`发送系统通知 - ${currentRoom?.name || ''}`" width="440px">
      <el-input v-model="notifyContent" type="textarea" :rows="3" placeholder="该通知将绕过审核，直接推送到聊天室" />
      <template #footer>
        <el-button @click="notifyDialog = false">取消</el-button>
        <el-button type="primary" @click="sendNotify">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Plus, User } from '@element-plus/icons-vue';
import { roomApi } from '../api';
import { useAuthStore } from '../stores/auth';
import PageHeader from '../components/PageHeader.vue';

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const query = reactive({ keyword: '', status: '', pageNo: 1, pageSize: 10 });
const page = reactive({ records: [], total: 0 });

const dialog = ref(false);
const editingId = ref(null);
const form = reactive({ name: '', description: '', maxUsers: 500, joinPolicy: 'OPEN', status: 'ACTIVE' });

const manageDialog = ref(false);
const memberLoading = ref(false);
const members = ref([]);
const currentRoom = ref(null);
const candidates = ref([]);
const candidateLoading = ref(false);
const selectedUserId = ref(null);

const notifyDialog = ref(false);
const notifyContent = ref('');

function statusText(s) {
  return { ACTIVE: '活跃', PAUSED: '暂停', CLOSED: '关闭' }[s] || s;
}
function statusTag(s) {
  return { ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'info' }[s] || '';
}
function memberStatusText(s) {
  return { JOINED: '已加入', PENDING: '待审批', LEFT: '已离开', REJECTED: '已拒绝' }[s] || s;
}
function memberStatusTag(s) {
  return { JOINED: 'success', PENDING: 'warning', LEFT: 'info', REJECTED: 'danger' }[s] || '';
}
function formatTime(t) {
  if (!t) return '-';
  return String(t).replace('T', ' ').slice(0, 19);
}

async function load() {
  loading.value = true;
  try {
    const res = await roomApi.page(query);
    page.records = res.records;
    page.total = res.total;
  } finally {
    loading.value = false;
  }
}

function onPageChange(p) {
  query.pageNo = p;
  load();
}

async function join(row) {
  const res = await roomApi.join(row.id);
  ElMessage.success(res.memberStatus === 'JOINED' ? '加入成功' : '已提交申请，等待审批');
  load();
}

async function leave(row) {
  await ElMessageBox.confirm(`确定退出「${row.name}」吗？`, '提示', { type: 'warning' });
  await roomApi.leave(row.id);
  ElMessage.success('已退出');
  load();
}

function enter(row) {
  router.push(`/app/rooms/${row.id}`);
}

function openCreate() {
  editingId.value = null;
  Object.assign(form, { name: '', description: '', maxUsers: 500, joinPolicy: 'OPEN', status: 'ACTIVE' });
  dialog.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    maxUsers: row.maxUsers,
    joinPolicy: row.joinPolicy,
    status: row.status,
  });
  dialog.value = true;
}

async function submitForm() {
  if (!form.name?.trim()) {
    ElMessage.warning('请输入聊天室名称');
    return;
  }
  if (editingId.value) {
    await roomApi.update(editingId.value, { ...form });
    ElMessage.success('修改成功');
  } else {
    const { status, ...createData } = form;
    await roomApi.create(createData);
    ElMessage.success('创建成功');
  }
  dialog.value = false;
  load();
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除聊天室「${row.name}」吗？此操作不可恢复。`, '删除确认', { type: 'warning' });
  await roomApi.remove(row.id);
  ElMessage.success('删除成功');
  load();
}

async function openMemberManage(row) {
  currentRoom.value = row;
  selectedUserId.value = null;
  candidates.value = [];
  manageDialog.value = true;
  await loadMembers();
}

async function loadMembers() {
  memberLoading.value = true;
  try {
    members.value = await roomApi.members(currentRoom.value.id);
  } finally {
    memberLoading.value = false;
  }
}

async function searchCandidates(keyword) {
  if (!keyword?.trim()) {
    candidates.value = [];
    return;
  }
  candidateLoading.value = true;
  try {
    candidates.value = await roomApi.memberCandidates(currentRoom.value.id, keyword.trim());
  } finally {
    candidateLoading.value = false;
  }
}

async function addMember() {
  if (!selectedUserId.value) return;
  await roomApi.addMember(currentRoom.value.id, selectedUserId.value);
  ElMessage.success('已拉入聊天室');
  selectedUserId.value = null;
  candidates.value = [];
  await loadMembers();
  load();
}

async function kickMember(row) {
  const name = row.nickname || row.username;
  await ElMessageBox.confirm(`确定将「${name}」移出聊天室吗？`, '移出确认', { type: 'warning' });
  await roomApi.kickMember(currentRoom.value.id, row.userId);
  ElMessage.success('已移出');
  await loadMembers();
  load();
}

async function approve(row, pass) {
  await roomApi.approveMember(currentRoom.value.id, row.userId, pass);
  ElMessage.success(pass ? '已通过' : '已拒绝');
  await loadMembers();
  load();
}

function openNotify(row) {
  currentRoom.value = row;
  notifyContent.value = '';
  notifyDialog.value = true;
}

async function sendNotify() {
  if (!notifyContent.value.trim()) {
    ElMessage.warning('请输入通知内容');
    return;
  }
  await roomApi.systemNotify(currentRoom.value.id, notifyContent.value);
  ElMessage.success('已发送');
  notifyDialog.value = false;
}

onMounted(load);
</script>

<style scoped>
.room-cell { display: flex; align-items: center; gap: 10px; }
.room-av { background: var(--brand-gradient); color: #fff; font-weight: 600; flex-shrink: 0; }
.room-nm { font-weight: 600; }
.manage-section { margin-bottom: 20px; }
.manage-section:last-child { margin-bottom: 0; }
.section-title { font-weight: 600; margin-bottom: 10px; color: var(--text-main); }
.add-row { display: flex; gap: 10px; align-items: center; }
.muted-tip { color: var(--text-secondary); font-size: 12px; }
</style>
