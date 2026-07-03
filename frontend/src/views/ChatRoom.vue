<template>
  <div class="chat">
    <div class="messages" ref="listEl">
      <div v-if="hasMore" class="load-more">
        <el-button link type="primary" :loading="loadingMore" @click="loadMore">加载更多历史消息</el-button>
      </div>
      <div v-else-if="messages.length" class="load-more no-more">没有更多历史了</div>
      <div v-for="m in messages" :key="m.messageId || m.id" class="msg" :class="{ system: m.type === 'NOTIFICATION' }">
        <span class="sender">{{ m.senderName }}</span>
        <span class="time">{{ formatTime(m.timestamp || m.submittedAt) }}</span>
        <div class="content">{{ m.content }}</div>
      </div>
    </div>
    <div class="input">
      <el-input v-model="draft" placeholder="输入消息，发送后需管理员审核" @keyup.enter="send" />
      <el-button type="primary" @click="send">发送</el-button>
    </div>
    <el-alert v-if="tip" :title="tip" type="info" show-icon :closable="false" style="margin-top:8px" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { messageApi } from '../api';
import { createChatSocket } from '../ws/useWebSocket';

const route = useRoute();
const roomId = route.params.id;
const messages = reactive([]);
const draft = ref('');
const tip = ref('');
const listEl = ref(null);
const hasMore = ref(false);
const loadingMore = ref(false);
const PAGE_SIZE = 30;
let socket = null;

function formatTime(t) {
  return t ? new Date(t).toLocaleTimeString() : '';
}

function scrollToBottom() {
  nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight; });
}

async function loadHistory() {
  const list = await messageApi.history(roomId, { size: PAGE_SIZE });
  hasMore.value = list.length >= PAGE_SIZE;
  messages.splice(0, messages.length, ...list.reverse());
  scrollToBottom();
}

async function loadMore() {
  const oldest = messages.find((m) => m.id);
  if (!oldest) return;
  loadingMore.value = true;
  try {
    const el = listEl.value;
    const prevHeight = el ? el.scrollHeight : 0;
    const list = await messageApi.history(roomId, { before: oldest.id, size: PAGE_SIZE });
    hasMore.value = list.length >= PAGE_SIZE;
    messages.unshift(...list.reverse());
    // 维持滚动位置，避免跳动
    nextTick(() => { if (el) el.scrollTop = el.scrollHeight - prevHeight; });
  } finally {
    loadingMore.value = false;
  }
}

async function send() {
  if (!draft.value.trim()) return;
  await messageApi.submit(roomId, draft.value);
  tip.value = '消息已提交，等待管理员审核通过后展示';
  draft.value = '';
}

onMounted(async () => {
  await loadHistory();
  socket = createChatSocket();
  socket.connect(localStorage.getItem('token'), {
    onConnect: () => {
      socket.subscribeRoom(roomId, (payload) => {
        messages.push(payload);
        scrollToBottom();
      });
      socket.subscribeNotifications((n) => ElMessage.info(n.content));
    },
  });
});

onUnmounted(() => socket?.disconnect());
</script>

<style scoped>
.chat { display: flex; flex-direction: column; height: calc(100vh - 120px); }
.messages { flex: 1; overflow-y: auto; background: #fff; border: 1px solid #eee; border-radius: 6px; padding: 12px; }
.load-more { text-align: center; margin-bottom: 8px; }
.load-more.no-more { color: #c0c4cc; font-size: 12px; }
.msg { margin-bottom: 12px; }
.msg.system .content { color: #e6a23c; }
.sender { font-weight: bold; margin-right: 8px; }
.time { color: #909399; font-size: 12px; }
.content { margin-top: 2px; }
.input { display: flex; gap: 8px; margin-top: 12px; }
</style>
