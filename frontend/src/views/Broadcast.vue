<template>
  <div class="page broadcast">
    <PageHeader title="消息广播" subtitle="向一个或多个聊天室推送通知（绕过审核，直接送达）" icon="Promotion" />

    <el-card shadow="never">
      <el-form label-width="90px">
        <el-form-item label="目标聊天室">
          <el-select
            v-model="roomIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择一个或多个聊天室"
            style="width:100%"
            :loading="roomsLoading"
          >
            <el-option
              v-for="r in rooms"
              :key="r.id"
              :label="`${r.name}（${statusText(r.status)}）`"
              :value="r.id"
              :disabled="r.status === 'CLOSED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="selectAll" @change="onSelectAll">全选（{{ selectableRooms.length }} 个可用聊天室）</el-checkbox>
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input v-model="content" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请输入广播内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="submit">发送广播</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Promotion, RefreshLeft } from '@element-plus/icons-vue';
import { roomApi } from '../api';
import PageHeader from '../components/PageHeader.vue';

const rooms = ref([]);
const roomsLoading = ref(false);
const roomIds = ref([]);
const content = ref('');
const sending = ref(false);
const selectAll = ref(false);

const selectableRooms = computed(() => rooms.value.filter((r) => r.status !== 'CLOSED'));

function statusText(s) {
  return { ACTIVE: '活跃', PAUSED: '暂停', CLOSED: '关闭' }[s] || s;
}

async function loadRooms() {
  roomsLoading.value = true;
  try {
    const res = await roomApi.page({ pageNo: 1, pageSize: 200 });
    rooms.value = res.records;
  } finally {
    roomsLoading.value = false;
  }
}

function onSelectAll(val) {
  roomIds.value = val ? selectableRooms.value.map((r) => r.id) : [];
}

function reset() {
  roomIds.value = [];
  content.value = '';
  selectAll.value = false;
}

async function submit() {
  if (!roomIds.value.length) {
    ElMessage.warning('请至少选择一个聊天室');
    return;
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入广播内容');
    return;
  }
  await ElMessageBox.confirm(`确定向 ${roomIds.value.length} 个聊天室广播该消息吗？`, '广播确认', { type: 'warning' });
  sending.value = true;
  try {
    await roomApi.broadcast({ roomIds: roomIds.value, content: content.value });
    ElMessage.success('广播已发送');
    reset();
  } finally {
    sending.value = false;
  }
}

onMounted(loadRooms);
</script>

<style scoped>
.card-header { display: flex; align-items: baseline; gap: 12px; }
.hint { color: #909399; font-size: 12px; }
</style>
