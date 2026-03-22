<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { ArrowLeft, Service, School } from '@element-plus/icons-vue'
import { sendAiCustomerMessage } from '../api/aiService'

const router = useRouter()
const chatScrollRef = ref(null)
const inputText = ref('')
const sending = ref(false)
const topNavActive = ref('ai-service')
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const unreadTotal = ref(0)
const currentUser = ref({
  id: null,
  nickname: '用户',
  avatarUrl: defaultAvatar,
})

/**
 * 聊天消息：
 * - role=user 表示用户消息
 * - role=assistant 表示 AI 客服回复
 */
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是 AI 客服。请告诉我你遇到的问题，我会尽力帮助你。',
    time: new Date().toISOString(),
  },
])

const canSend = computed(() => !sending.value && inputText.value.trim().length > 0)

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return ''
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

const isMe = (m) => m?.role === 'user'

const scrollToBottom = () => {
  nextTick(() => {
    const el = chatScrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const pushLocalMessage = (role, content) => {
  messages.value.push({
    role,
    content,
    time: new Date().toISOString(),
  })
  scrollToBottom()
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  inputText.value = ''
  pushLocalMessage('user', text)
  sending.value = true

  try {
    const resp = await sendAiCustomerMessage({
      message: text,
    })
    const reply = resp?.reply || '客服暂时没有返回内容，请稍后重试。'
    pushLocalMessage('assistant', reply)
  } catch (_) {
    // 后端接口未完成时，先用兜底文案保证前端可演示
    pushLocalMessage('assistant', '接口预留中：后端 AI 客服服务尚未接入。')
    ElMessage.warning('AI 客服接口暂未接通，已使用占位回复。')
  } finally {
    sending.value = false
  }
}

const refreshUnreadTotal = async () => {
  if (!currentUser.value?.id) {
    unreadTotal.value = 0
    return
  }
  try {
    const { data } = await axios.get('/api/messages/unread-total', {
      params: { userId: currentUser.value.id },
    })
    unreadTotal.value = Number.isFinite(Number(data)) ? Number(data) : 0
  } catch {
    // ignore
  }
}

const handleTopNavSelect = (key) => {
  if (key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (key === 'market') {
    router.push('/market')
    return
  }
  if (key === 'messages') {
    router.push('/messages')
    return
  }
  if (key === 'profile') {
    router.push('/profile')
  }
}

const handleEnterSend = (e) => {
  e?.preventDefault?.()
  sendMessage()
}

onMounted(() => {
  const raw = localStorage.getItem('currentUser')
  if (raw) {
    try {
      const u = JSON.parse(raw)
      currentUser.value = {
        id: u?.id ?? null,
        nickname: u?.nickname || u?.username || '用户',
        avatarUrl: u?.avatarUrl || defaultAvatar,
      }
    } catch {
      // ignore
    }
  }
  refreshUnreadTotal()
  scrollToBottom()
})
</script>

<template>
  <div class="ai-shell">
    <header class="ai-nav-bar">
      <div class="ai-nav-inner">
        <div class="ai-nav-brand">
          <el-icon class="ai-nav-logo"><School /></el-icon>
          <span class="ai-nav-title">校园即时服务系统</span>
        </div>
        <el-menu
          class="ai-nav-menu"
          mode="horizontal"
          :default-active="topNavActive"
          @select="handleTopNavSelect"
        >
          <el-menu-item index="tasks">任务大厅</el-menu-item>
          <el-menu-item index="market">交易市场</el-menu-item>
          <el-menu-item index="messages">
            <span class="nav-item-label">
              我的消息
              <span v-if="unreadTotal > 0" class="nav-badge">{{ unreadTotal }}</span>
            </span>
          </el-menu-item>
          <el-menu-item index="profile">个人中心</el-menu-item>
          <el-menu-item index="ai-service">AI 客服</el-menu-item>
        </el-menu>
        <div class="ai-nav-right">
          <div class="ai-nav-user" @click="router.push('/profile')">
            <span class="ai-nav-name">{{ currentUser.nickname }}</span>
            <img :src="currentUser.avatarUrl || defaultAvatar" class="ai-nav-avatar" alt="头像" />
          </div>
        </div>
      </div>
    </header>

    <section class="ai-chat-shell">
      <header class="ai-topbar">
        <button class="back-btn" type="button" @click="router.push('/messages')">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="topbar-title">
          <div class="topbar-main">
            <el-icon><Service /></el-icon>
            <span>AI 客服</span>
          </div>
          <div class="topbar-sub">在线问答</div>
        </div>
        <div class="topbar-right" />
      </header>

      <section class="chat">
        <div class="chat-scroll" ref="chatScrollRef">
          <div
            v-for="(m, idx) in messages"
            :key="idx"
            class="msg-row"
            :class="{ me: isMe(m) }"
          >
            <div class="bubble">
              <div class="bubble-text">{{ m.content }}</div>
              <div class="bubble-time">{{ formatTime(m.time) }}</div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入你的问题，Enter 发送（Shift+Enter 换行）"
            @keydown.enter.exact="handleEnterSend"
          />
          <div class="chat-actions">
            <div class="hint">AI 客服在线答疑</div>
            <el-button type="primary" :disabled="!canSend" :loading="sending" @click="sendMessage">
              发送
            </el-button>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.ai-shell {
  height: 100vh;
  background:
    radial-gradient(1200px 520px at 10% 0%, rgba(99, 102, 241, 0.1), transparent 60%),
    radial-gradient(900px 420px at 95% 20%, rgba(56, 189, 248, 0.1), transparent 55%),
    linear-gradient(180deg, #f8fafc, #f1f5f9);
  color: #0f172a;
}
.ai-nav-bar { padding: 10px 18px 4px; background: rgba(255, 255, 255, 0.96); backdrop-filter: blur(14px); }
.ai-nav-inner { max-width: 1120px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.ai-nav-brand { display: flex; align-items: center; gap: 8px; }
.ai-nav-logo { font-size: 18px; color: #2563eb; }
.ai-nav-title { font-size: 14px; font-weight: 600; color: #111827; }
.ai-nav-menu { border-bottom: none; flex: 1; display: flex; justify-content: center; }
.ai-nav-right { display: flex; align-items: center; margin-left: 12px; }
.ai-nav-user { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 8px; border-radius: 999px; transition: background 0.2s ease; }
.ai-nav-user:hover { background: rgba(37, 99, 235, 0.08); }
.ai-nav-name { font-size: 13px; font-weight: 600; color: #111827; max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-nav-avatar { width: 32px; height: 32px; border-radius: 999px; object-fit: cover; border: 2px solid rgba(37, 99, 235, 0.3); }
.nav-item-label { position: relative; display: inline-flex; align-items: center; gap: 4px; }
.nav-badge { min-width: 16px; height: 16px; padding: 0 4px; border-radius: 999px; background: #ef4444; color: #fff; font-size: 11px; line-height: 16px; display: inline-flex; align-items: center; justify-content: center; }

.ai-chat-shell {
  max-width: 1120px;
  height: calc(100vh - 56px);
  margin: 6px auto 0;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
}
.ai-topbar { height: 58px; display: grid; grid-template-columns: auto 1fr auto; align-items: center; padding: 0 14px; gap: 10px; border-bottom: 1px solid rgba(226, 232, 240, 0.9); background: rgba(255, 255, 255, 0.82); }
.back-btn { width: 38px; height: 38px; border-radius: 999px; border: 1px solid rgba(226, 232, 240, 0.9); background: #fff; display: grid; place-items: center; cursor: pointer; }
.topbar-main { font-weight: 800; color: #0f172a; display: flex; align-items: center; gap: 6px; }
.topbar-sub { font-size: 12px; color: #64748b; margin-top: 2px; }
.chat { height: calc(100% - 58px); display: flex; flex-direction: column; }
.chat-scroll { flex: 1; overflow: auto; padding: 18px; }
.msg-row { display: flex; justify-content: flex-start; margin: 12px 0; }
.msg-row.me { justify-content: flex-end; }
.bubble { max-width: 620px; padding: 10px 12px; border-radius: 14px; background: rgba(255, 255, 255, 0.92); border: 1px solid rgba(226, 232, 240, 0.9); box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06); }
.msg-row.me .bubble { background: rgba(37, 99, 235, 0.1); border-color: rgba(37, 99, 235, 0.2); }
.bubble-text { white-space: pre-wrap; word-break: break-word; font-size: 13px; color: #0f172a; line-height: 1.55; }
.bubble-time { margin-top: 6px; font-size: 11px; color: #94a3b8; }
.chat-input { padding: 12px 14px 14px; border-top: 1px solid rgba(226, 232, 240, 0.9); background: rgba(255, 255, 255, 0.82); }
.chat-actions { margin-top: 10px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.hint { font-size: 12px; color: #64748b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 900px) {
  .ai-nav-inner { flex-direction: column; align-items: flex-start; }
  .ai-nav-menu { justify-content: flex-start; margin-left: 0; overflow-x: auto; }
  .ai-chat-shell { margin: 6px 12px 0; height: calc(100vh - 72px); }
}
</style>

