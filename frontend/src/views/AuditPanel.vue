<template>
  <div>
    <div class="toolbar">
      <el-input v-model.number="roomId" placeholder="按聊天室ID筛选" style="width:180px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">刷新</el-button>
      <el-button type="success" :disabled="!selection.length" @click="batch('APPROVE')">批量通过</el-button>
      <el-button type="danger" :disabled="!selection.length" @click="batch('REJECT')">批量拒绝</el-button>
      <el-tag type="info">待审核：{{ page.total }}</el-tag>
    </div>

    <el-table :data="page.records" @selection-change="(v) => (selection = v)">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="senderName" label="提交者" width="120" />
      <el-table-column prop="roomId" label="聊天室" width="90" />
      <el-table-column prop="content" label="内容" />
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ new Date(row.submittedAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="success" link @click="approve(row)">通过</el-button>
          <el-button type="danger" link @click="reject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="prev, pager, next, total, sizes"
      :total="page.total"
      :page-size="pageSize"
      :current-page="pageNo"
      :page-sizes="[10, 20, 50]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { auditApi } from '../api';
import { createChatSocket } from '../ws/useWebSocket';

const roomId = ref(null);
const page = reactive({ records: [], total: 0 });
const selection = ref([]);
const pageNo = ref(1);
const pageSize = ref(10);
let socket = null;

async function load() {
  const res = await auditApi.pending({
    roomId: roomId.value || undefined,
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  });
  page.records = res.records;
  page.total = res.total;
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

async function approve(row) {
  await auditApi.approve(row.id);
  ElMessage.success('已通过');
  load();
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝', { inputPlaceholder: '可选' }).catch(() => ({}));
  await auditApi.reject(row.id, value || '');
  ElMessage.success('已拒绝');
  load();
}

async function batch(action) {
  await auditApi.batch({ messageIds: selection.value.map((r) => r.id), action });
  ElMessage.success('批量操作完成');
  load();
}

onMounted(() => {
  load();
  socket = createChatSocket();
  socket.connect(localStorage.getItem('token'), {
    onConnect: () => socket.subscribeAudit(() => load()),
  });
});

onUnmounted(() => socket?.disconnect());
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
</style>
