<template>
  <div class="chat-page">
    <!-- 顶部房间信息 -->
    <div class="chat-header">
      <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
      <el-avatar :size="38" class="head-av">{{ (room.name || '?').charAt(0) }}</el-avatar>
      <div class="head-info">
        <div class="head-name">{{ room.name || '聊天室' }}</div>
        <div class="head-sub">
          <span class="dot" :class="room.status === 'ACTIVE' ? 'on' : 'off'"></span>
          {{ statusText(room.status) }} · {{ room.memberCount ?? '—' }}/{{ room.maxUsers ?? '—' }} 人
        </div>
      </div>
    </div>

    <!-- 消息区 -->
    <div class="messages" ref="listEl">
      <div v-if="hasMore" class="load-more">
        <el-button link type="primary" :loading="loadingMore" @click="loadMore">加载更多历史消息</el-button>
      </div>
      <div v-else-if="messages.length" class="load-more no-more">— 没有更多历史了 —</div>

      <template v-for="m in messages" :key="m.messageId || m.id">
        <!-- 系统通知 -->
        <div v-if="m.type === 'NOTIFICATION'" class="sys-row">
          <span class="sys-pill">{{ m.content }}</span>
        </div>
        <!-- 普通消息 -->
        <div v-else class="msg-row" :class="{ mine: isMine(m) }">
          <el-avatar :size="36" class="msg-av">{{ (m.senderName || '?').charAt(0) }}</el-avatar>
          <div class="msg-main">
            <div class="msg-meta">
              <span class="msg-sender">{{ m.senderName }}</span>
              <span class="msg-time">{{ formatTime(m.timestamp || m.submittedAt) }}</span>
            </div>
            <div class="bubble">{{ m.content }}</div>
          </div>
        </div>
      </template>

      <el-empty v-if="!messages.length" description="还没有消息，来说点什么吧" :image-size="90" />
    </div>

    <!-- 输入区 -->
    <div class="composer">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        resize="none"
        :maxlength="messageMaxLength"
        placeholder="输入消息，回车发送（发送后需管理员审核）"
        @keyup.enter.exact.prevent="send"
      />
      <el-button type="primary" :icon="Promotion" :disabled="!draft.trim()" @click="send">发送</el-button>
    </div>
    <div v-if="tip" class="composer-tip"><el-icon><InfoFilled /></el-icon> {{ tip }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Promotion, InfoFilled } from '@element-plus/icons-vue';
import { messageApi, roomApi, configApi } from '../api';
import { useAuthStore } from '../stores/auth';
import { useWsStore } from '../stores/ws';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const ws = useWsStore();
const roomId = route.params.id;
const messages = reactive([]);
const room = reactive({ name: '', status: '', memberCount: null, maxUsers: null });
const draft = ref('');
const tip = ref('');
const listEl = ref(null);
const hasMore = ref(false);
const loadingMore = ref(false);
const messageMaxLength = ref(1000);
const PAGE_SIZE = 30;

function appendMessage(payload) {
  const msg = {
    ...payload,
    messageId: payload.messageId ?? payload.id,
    id: payload.messageId ?? payload.id,
  };
  messages.push(msg);
  scrollToBottom();
}

function isMine(m) {
  return m.senderId != null && m.senderId === auth.user?.id;
}
function statusText(s) {
  return { ACTIVE: '活跃', PAUSED: '暂停', CLOSED: '已关闭' }[s] || '加载中';
}
function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function goBack() {
  router.push('/app/rooms');
}

function scrollToBottom() {
  nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight; });
}

async function loadRoom() {
  try {
    const d = await roomApi.detail(roomId);
    Object.assign(room, d);
  } catch (e) {
    // 忽略，头部显示占位
  }
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
  try {
    const cfg = await configApi.runtime();
    messageMaxLength.value = cfg.messageMaxLength;
  } catch (e) {
    // 使用默认值
  }
  await loadRoom();
  await loadHistory();
  await ws.ensureConnected();
  ws.subscribeRoom(roomId, appendMessage);
});

onUnmounted(() => {
  ws.unsubscribeRoom(roomId);
});
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border-soft);
  background: #fbfcfe;
}
.head-av { background: var(--brand-gradient); color: #fff; font-weight: 600; }
.head-info { line-height: 1.3; }
.head-name { font-weight: 700; font-size: 15px; }
.head-sub { font-size: 12px; color: var(--text-secondary); display: flex; align-items: center; gap: 6px; }
.dot { width: 7px; height: 7px; border-radius: 50%; display: inline-block; }
.dot.on { background: #12b76a; box-shadow: 0 0 0 3px rgba(18,183,106,0.15); }
.dot.off { background: #c0c4cc; }

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 22px;
  background: #f7f9fc;
}
.load-more { text-align: center; margin-bottom: 14px; }
.load-more.no-more { color: #c0c4cc; font-size: 12px; }

.sys-row { text-align: center; margin: 14px 0; }
.sys-pill {
  display: inline-block;
  background: rgba(146, 161, 181, 0.16);
  color: var(--text-secondary);
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 20px;
}

.msg-row { display: flex; gap: 10px; margin-bottom: 18px; align-items: flex-start; }
.msg-av { flex-shrink: 0; background: #dfe4ef; color: #5a6b82; font-weight: 600; }
.msg-main { max-width: 68%; }
.msg-meta { display: flex; align-items: baseline; gap: 8px; margin-bottom: 4px; }
.msg-sender { font-size: 13px; font-weight: 600; color: var(--text-regular); }
.msg-time { font-size: 11px; color: var(--text-secondary); }
.bubble {
  background: #fff;
  border: 1px solid var(--border-soft);
  padding: 10px 14px;
  border-radius: 4px 14px 14px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-main);
  word-break: break-word;
  white-space: pre-wrap;
  box-shadow: 0 2px 8px rgba(26,42,82,0.04);
}

/* 自己发送的消息靠右 */
.msg-row.mine { flex-direction: row-reverse; }
.msg-row.mine .msg-av { background: var(--brand-gradient); color: #fff; }
.msg-row.mine .msg-meta { flex-direction: row-reverse; }
.msg-row.mine .bubble {
  background: var(--brand-gradient);
  color: #fff;
  border: none;
  border-radius: 14px 4px 14px 14px;
}

.composer {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--border-soft);
  background: #fff;
}
.composer .el-textarea { flex: 1; }
.composer .el-button { height: 40px; }
.composer-tip {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 18px 12px;
  font-size: 12px; color: var(--brand-1);
  background: #fff;
}
</style>
