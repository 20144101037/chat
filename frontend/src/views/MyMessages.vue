<template>
  <div class="page">
    <PageHeader title="我的消息" subtitle="查看自己发送的消息及其审核状态" icon="ChatLineSquare">
      <template #actions>
        <el-input v-model="keyword" placeholder="按内容过滤（当前页）" style="width:220px" clearable :prefix-icon="Search" />
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="onFilter">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PENDING_REVIEW">待审核</el-radio-button>
        <el-radio-button label="APPROVED">已通过</el-radio-button>
        <el-radio-button label="REJECTED">已拒绝</el-radio-button>
        <el-radio-button label="TIMEOUT">超时</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="filtered" v-loading="loading" style="width:100%" empty-text="暂无消息">
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="聊天室" width="120">
        <template #default="{ row }">{{ roomName(row.roomId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" width="180">
        <template #default="{ row }">{{ formatTime(row.reviewedAt) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="prev, pager, next, total, sizes"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNo"
      :page-sizes="[10, 20, 50]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Search, Refresh } from '@element-plus/icons-vue';
import { messageApi, roomApi } from '../api';
import PageHeader from '../components/PageHeader.vue';

const loading = ref(false);
const status = ref('');
const keyword = ref('');
const list = ref([]);
const roomMap = ref({});
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);

const filtered = computed(() => {
  const kw = keyword.value.trim();
  if (!kw) return list.value;
  return list.value.filter((m) => (m.content || '').includes(kw));
});

function statusText(s) {
  return { PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', TIMEOUT: '超时' }[s] || s;
}
function statusTag(s) {
  return { PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', TIMEOUT: 'info' }[s] || '';
}
function formatTime(t) {
  return t ? new Date(t).toLocaleString() : '-';
}
function roomName(id) {
  return roomMap.value[id] || `#${id}`;
}

async function loadRooms() {
  const res = await roomApi.page({ pageNo: 1, pageSize: 200 });
  const map = {};
  res.records.forEach((r) => { map[r.id] = r.name; });
  roomMap.value = map;
}

async function load() {
  loading.value = true;
  try {
    const res = await messageApi.myMessages({
      status: status.value || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    });
    list.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function onFilter() {
  pageNo.value = 1;
  load();
}
function onPageChange(p) {
  pageNo.value = p;
  load();
}
function onSizeChange(s) {
  pageSize.value = s;
  pageNo.value = 1;
  load();
}

onMounted(async () => {
  await loadRooms();
  await load();
});
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; }
</style>
