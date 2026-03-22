<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { Search, Tickets, ShoppingBag, ArrowLeft, School } from '@element-plus/icons-vue'

const router = useRouter()

const currentUserId = ref(null)
const currentUser = ref({
  id: null,
  nickname: '',
  avatarUrl: '',
})

/**
 * 顶部全局导航当前激活菜单。
 * 说明：
 * - 用于 Element Plus 顶部菜单，使“我的消息”页与首页/交易市场/个人中心保持统一导航体验。
 */
const topNavActive = ref('messages')

const activeType = ref('TASK') // TASK / GOODS / AI
const keyword = ref('')

const conversationsTask = ref([])
const conversationsGoods = ref([])
const loadingConversations = ref(false)

const activeConv = ref(null) // { type, bizId, otherUserId, title, ... }
const messages = ref([])
const loadingMessages = ref(false)

const inputText = ref('')
const sending = ref(false)
const chatInputRef = ref(null)
// 中文输入法组合输入时，textarea.value 可能尚未提交到 v-model
const isComposing = ref(false)

const onCompositionStart = () => {
  isComposing.value = true
}
const onCompositionEnd = () => {
  isComposing.value = false
}

const getCommittedInputText = () => {
  // 先以 v-model 为准
  const t1 = (inputText.value || '').trim()
  if (t1) return t1
  // 兜底：从 Element Plus el-input 的实际 textarea 读取（解决 IME 未上屏/同步延迟）
  try {
    const el = chatInputRef.value?.$el || chatInputRef.value?.$?.vnode?.el || null
    const ta = el?.querySelector?.('textarea')
    const t2 = (ta?.value || '').trim()
    if (t2) return t2
  } catch (_) {}
  // 再兜底：从当前聚焦元素读取
  try {
    const active = document.activeElement
    if (active && active.tagName === 'TEXTAREA') {
      const t3 = (active.value || '').trim()
      if (t3) return t3
    }
  } catch (_) {}
  return ''
}

const handleEnterSend = (e) => {
  if (isComposing.value) return
  e?.preventDefault?.()
  sendMessage()
}

let ws = null
const wsConnected = ref(false)
const chatScrollRef = ref(null)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

/** 顶部“我的消息”未读总数（任务 + 商品） */
const unreadTotal = ref(0)

const refreshUnreadTotal = async () => {
  if (!currentUserId.value) {
    unreadTotal.value = 0
    return
  }
  try {
    const { data } = await axios.get('/api/messages/unread-total', {
      params: { userId: currentUserId.value },
    })
    unreadTotal.value = Number.isFinite(Number(data)) ? Number(data) : 0
  } catch {
    // 静默失败
  }
}

/** 任务消息、商品交易各自的未读总数（来自会话列表的 unreadCount 求和） */
const unreadTaskTotal = computed(() =>
  (conversationsTask.value || []).reduce(
    (sum, c) => sum + (c.unreadCount || 0),
    0,
  ),
)

const unreadGoodsTotal = computed(() =>
  (conversationsGoods.value || []).reduce(
    (sum, c) => sum + (c.unreadCount || 0),
    0,
  ),
)

const allConversations = computed(() => {
  let src
  if (activeType.value === 'TASK') {
    src = conversationsTask.value
  } else {
    src = conversationsGoods.value
  }
  const list = Array.isArray(src) ? src.slice() : []
  list.sort((a, b) => String(a.title || '').localeCompare(String(b.title || ''), 'zh-Hans-CN'))
  const kw = keyword.value.trim()
  if (!kw) return list
  return list.filter((c) => {
    const t = `${c.title || ''} ${c.otherNickname || ''} ${c.lastContent || ''}`
    return t.toLowerCase().includes(kw.toLowerCase())
  })
})

const activeHeader = computed(() => {
  if (!activeConv.value) return { title: '请选择会话', sub: '' }
  const t = activeConv.value.type === 'TASK' ? '任务消息' : '商品交易'
  return { title: activeConv.value.title || '会话', sub: `${t} · ${activeConv.value.otherNickname || '对方'}` }
})

/** 判断消息是否为自己发送（AI 会话用 role/senderId） */
const isMe = (m) => {
  if (!m || !activeConv.value) return false
  return String(m.senderId) !== String(activeConv.value.otherUserId)
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = chatScrollRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const buildWsUrl = () => {
  const userId = currentUserId.value
  if (!userId) return null
  const isHttps = window.location.protocol === 'https:'
  const wsProto = isHttps ? 'wss' : 'ws'
  // 通过 Vite dev 服务器或线上反向代理转发 /ws 到后端
  // 前端始终连当前域名的 /ws/chat，避免直接写死 8080，方便你用内网穿透把 5173 暴露到公网
  const host = window.location.host
  return `${wsProto}://${host}/ws/chat?userId=${encodeURIComponent(userId)}`
}

const connectWs = () => {
  const url = buildWsUrl()
  if (!url) return
  try {
    ws = new WebSocket(url)
    wsConnected.value = false
    ws.onopen = () => {
      wsConnected.value = true
    }
    ws.onclose = () => {
      wsConnected.value = false
      ws = null
      window.setTimeout(() => connectWs(), 1500)
    }
    ws.onerror = () => {
    }
    ws.onmessage = async (evt) => {
      try {
        const payload = JSON.parse(evt.data)
        // payload: ChatMessageItem
        if (!payload || !payload.type || !payload.bizId) return
        const type = String(payload.type).toUpperCase()
        const listRef = type === 'TASK' ? conversationsTask : conversationsGoods
        const idx = listRef.value.findIndex((c) => c.bizId === payload.bizId && String(c.type).toUpperCase() === type)
        if (idx >= 0) {
          const c = { ...listRef.value[idx] }
          c.lastContent = payload.content
          c.lastTime = payload.createTime || new Date().toISOString()
          const isActive = activeConv.value
            && String(activeConv.value.type).toUpperCase() === type
            && Number(activeConv.value.bizId) === Number(payload.bizId)
          if (!isActive) c.unreadCount = (c.unreadCount || 0) + 1
          listRef.value.splice(idx, 1, c)
        }
        const isActiveNow = activeConv.value
          && String(activeConv.value.type).toUpperCase() === type
          && Number(activeConv.value.bizId) === Number(payload.bizId)
        if (isActiveNow) {
          messages.value.push(payload)
          await markRead(activeConv.value)
        }
        // 有任何新消息到达时，刷新未读总数，驱动顶部导航栏和悬浮窗红点
        await refreshUnreadTotal()
      } catch (_) {
        // ignore
      }
    }
  } catch (e) {
    wsConnected.value = false
  }
}

const loadConversations = async () => {
  if (!currentUserId.value) return
  loadingConversations.value = true
  try {
    const [taskRes, goodsRes] = await Promise.all([
      axios.get('/api/messages/conversations', { params: { userId: currentUserId.value, type: 'TASK' } }),
      axios.get('/api/messages/conversations', { params: { userId: currentUserId.value, type: 'GOODS' } }),
    ])
    conversationsTask.value = taskRes.data || []
    conversationsGoods.value = goodsRes.data || []
  } catch (e) {
    const msg = e?.response?.data || e?.message || '加载会话失败'
    ElMessage.error(msg)
    conversationsTask.value = conversationsTask.value || []
    conversationsGoods.value = conversationsGoods.value || []
  } finally {
    loadingConversations.value = false
  }
}

const loadHistory = async (conv) => {
  if (!conv || !currentUserId.value) return
  loadingMessages.value = true
  try {
    const { data } = await axios.get('/api/messages/history', {
      params: {
        userId: currentUserId.value,
        type: conv.type,
        bizId: conv.bizId,
        otherUserId: conv.otherUserId,
        limit: 200,
      },
    })
    messages.value = Array.isArray(data) ? data : []
  } catch (e) {
    const msg = e?.response?.data || '加载聊天记录失败'
    ElMessage.error(msg)
  } finally {
    loadingMessages.value = false
  }
}

const markRead = async (conv) => {
  if (!conv || !currentUserId.value) return
  try {
    await axios.post('/api/messages/read', null, {
      params: {
        userId: currentUserId.value,
        type: conv.type,
        bizId: conv.bizId,
        otherUserId: conv.otherUserId,
      },
    })
    const listRef = String(conv.type).toUpperCase() === 'TASK' ? conversationsTask : conversationsGoods
    const idx = listRef.value.findIndex((c) => c.bizId === conv.bizId)
    if (idx >= 0) {
      const c = { ...listRef.value[idx], unreadCount: 0 }
      listRef.value.splice(idx, 1, c)
    }
  } catch (_) {
    // ignore
  }
}

const openConversation = async (conv) => {
  if (!conv) return
  activeConv.value = conv
  await loadHistory(conv)
  await markRead(conv)
  // 打开会话后自动聚焦输入框，方便立即输入
  nextTick(() => {
    chatInputRef.value?.focus?.()
  })
}

const sendMessage = async () => {
  const conv = activeConv.value
  if (!conv) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  const text = getCommittedInputText()
  if (!text) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  sending.value = true
  try {
    const { data } = await axios.post('/api/messages/send', {
      type: conv.type,
      bizId: conv.bizId,
      senderId: currentUserId.value,
      receiverId: conv.otherUserId,
      content: text,
    })
    messages.value.push(data)
    inputText.value = ''
    try {
      const el = chatInputRef.value?.$el
      const ta = el?.querySelector?.('textarea')
      if (ta) ta.value = ''
    } catch (_) {}
    const listRef = String(conv.type).toUpperCase() === 'TASK' ? conversationsTask : conversationsGoods
    const idx = listRef.value.findIndex((c) => c.bizId === conv.bizId)
    if (idx >= 0) {
      const c = { ...listRef.value[idx] }
      c.lastContent = text
      c.lastTime = data?.createTime || new Date().toISOString()
      listRef.value.splice(idx, 1, c)
    }
  } catch (e) {
    const raw = e?.response?.data ?? e?.message ?? '发送失败'
    const msg = typeof raw === 'string' ? raw : JSON.stringify(raw)
    ElMessage.error(msg)
  } finally {
    sending.value = false
  }
}

/**
 * 清空当前会话的聊天记录：调用 /api/messages/clear 并重置右侧列表。
 */
const handleClearHistory = async () => {
  if (!activeConv.value || !currentUserId.value) return
  try {
    await ElMessageBox.confirm('确认清空当前会话的聊天记录？', '清空聊天', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  const conv = activeConv.value
  try {
    await axios.post('/api/messages/clear', null, {
      params: {
        userId: currentUserId.value,
        type: conv.type,
        bizId: conv.bizId,
        otherUserId: conv.otherUserId,
      },
    })
  } catch (e) {
    const raw = e?.response?.data ?? e?.message ?? '清空失败'
    const msg = typeof raw === 'string' ? raw : JSON.stringify(raw)
    ElMessage.error(msg)
    return
  }
  messages.value = []
  const listRef = String(conv.type).toUpperCase() === 'TASK' ? conversationsTask : conversationsGoods
  const idx = listRef.value.findIndex(
    (c) =>
      c.bizId === conv.bizId
      && String(c.type).toUpperCase() === String(conv.type).toUpperCase(),
  )
  if (idx >= 0) {
    const item = { ...listRef.value[idx] }
    item.lastContent = ''
    item.lastTime = null
    item.unreadCount = 0
    listRef.value.splice(idx, 1, item)
  }
  await refreshUnreadTotal()
}

/**
 * 顶部导航点击处理。
 *
 * @param {string} key 菜单 key：tasks / market / messages / profile
 */
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
    // 当前页，无需跳转
    return
  }
  if (key === 'profile') {
    router.push('/profile')
    return
  }
}

watch(activeType, async () => {
  // 切换任务 / 商品标签时重置当前会话；AI 标签由 openConversation 自己控制
  activeConv.value = null
  messages.value = []
})

// 每当当前会话的消息列表变化时，自动滚动到底部（进入会话 / 发送 / 收到新消息）
watch(
  messages,
  () => {
    if (activeConv.value) scrollToBottom()
  },
  { deep: true }
)

onMounted(async () => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) {
    router.push('/login')
    return
  }
  try {
    const u = JSON.parse(raw)
    currentUserId.value = u?.id ?? null
    currentUser.value = {
      id: u?.id ?? null,
      nickname: u?.nickname || u?.username || '??',
      avatarUrl: u?.avatarUrl || defaultAvatar,
    }
  } catch (_) {
    router.push('/login')
    return
  }
  await loadConversations()
  await refreshUnreadTotal()

  const query = router.currentRoute.value.query
  const queryType = String(query.type || '').toUpperCase()
  if (queryType === 'GOODS' || queryType === 'TASK') {
    activeType.value = queryType
  }
  // AI 客服已移除：不再支持 openAi 参数

  connectWs()
})

onUnmounted(() => {
  try {
    ws?.close?.()
  } catch (_) {}
  ws = null
})
</script>

<template>
  <div class="msg-shell">
    <!-- 顶部全局导航栏 -->
    <header class="msg-nav-bar">
      <div class="msg-nav-inner">
        <div class="msg-nav-brand">
          <el-icon class="msg-nav-logo"><School /></el-icon>
          <span class="msg-nav-title">校园即时服务系统</span>
        </div>
        <el-menu
          class="msg-nav-menu"
          mode="horizontal"
          :default-active="topNavActive"
          @select="handleTopNavSelect"
        >
          <el-menu-item index="tasks">任务大厅</el-menu-item>
          <el-menu-item index="market">交易市场</el-menu-item>
          <el-menu-item index="messages">
            <span class="nav-item-label">
              我的消息
              <span
                v-if="unreadTotal > 0"
                class="nav-badge"
              >
                {{ unreadTotal }}
              </span>
            </span>
          </el-menu-item>
          <el-menu-item index="profile">个人中心</el-menu-item>
        </el-menu>
        <div class="msg-nav-right">
          <div class="msg-nav-user" @click="router.push('/profile')">
            <span class="msg-nav-name">{{ currentUser.nickname }}</span>
            <img :src="currentUser.avatarUrl || defaultAvatar" class="msg-nav-avatar" alt="头像" />
          </div>
        </div>
      </div>
    </header>

    <!-- 消息页自身顶部信息条 -->
    <header class="msg-topbar">
      <button class="back-btn" type="button" @click="router.push('/home')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="topbar-title">
        <div class="topbar-main">我的消息</div>
        <div class="topbar-sub">
          {{ wsConnected ? '实时连接中' : '连接中…' }}
        </div>
      </div>
      <div class="topbar-right"></div>
    </header>

    <div class="msg-frame">
      <aside class="sidebar">
        <div class="sidebar-tabs">
          <button
            type="button"
            class="tab"
            :class="{ active: activeType === 'TASK' }"
            @click="activeType = 'TASK'"
          >
            <el-icon><Tickets /></el-icon>
            <span class="tab-label">
              任务消息
              <span
                v-if="unreadTaskTotal > 0"
                class="tab-badge"
              >
                {{ unreadTaskTotal }}
              </span>
            </span>
          </button>
          <button
            type="button"
            class="tab"
            :class="{ active: activeType === 'GOODS' }"
            @click="activeType = 'GOODS'"
          >
            <el-icon><ShoppingBag /></el-icon>
            <span class="tab-label">
              商品交易
              <span
                v-if="unreadGoodsTotal > 0"
                class="tab-badge"
              >
                {{ unreadGoodsTotal }}
              </span>
            </span>
          </button>
          <!-- AI 客服已移除 -->
        </div>

        <div class="sidebar-search">
          <el-input v-model="keyword" placeholder="搜索 标题/对方/内容" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="sidebar-list">
          <div v-if="loadingConversations" class="list-loading">加载会话中…</div>
          <div v-else-if="allConversations.length === 0" class="list-empty">
            <el-icon class="empty-icon"><Tickets /></el-icon>
            <div class="empty-title">暂无会话</div>
            <div class="empty-sub">接单或发生交易后，会话会出现在这里</div>
          </div>
          <button
            v-else
            v-for="c in allConversations"
            :key="`${c.type}-${c.bizId}`"
            type="button"
            class="conv-item"
            :class="{ active: activeConv && activeConv.type === c.type && activeConv.bizId === c.bizId }"
            @click="openConversation(c)"
          >
            <div class="conv-avatar">
              <img :src="c.otherAvatarUrl || defaultAvatar" alt="avatar" />
              <span v-if="c.unreadCount" class="badge">{{ c.unreadCount }}</span>
            </div>
            <div class="conv-main">
              <div class="conv-title-row">
                <div class="conv-title">{{ c.title }}</div>
                <div class="conv-time">{{ formatTime(c.lastTime) }}</div>
              </div>
              <div class="conv-sub-row">
                <div class="conv-sub">{{ c.otherNickname || '对方' }}</div>
                <div class="conv-last">{{ c.lastContent || '暂无消息' }}</div>
              </div>
            </div>
          </button>
        </div>
      </aside>

      <section class="chat">
        <div class="chat-head">
          <div>
            <div class="chat-head-title">{{ activeHeader.title }}</div>
            <div class="chat-head-sub">{{ activeHeader.sub }}</div>
          </div>
          <el-button
            v-if="activeConv"
            class="chat-head-clear"
            size="small"
            text
            type="danger"
            @click="handleClearHistory"
          >
            清空聊天
          </el-button>
        </div>

        <div class="chat-body">
          <div v-if="!activeConv" class="chat-placeholder">
            <div class="placeholder-card">
              <div class="placeholder-title">选择一个会话开始聊天</div>
              <div class="placeholder-sub">左侧按「标题」排列，点击进入对话窗口</div>
            </div>
          </div>
          <div v-else class="chat-scroll" ref="chatScrollRef">
            <div v-if="loadingMessages" class="chat-loading">加载中…</div>
            <div v-else>
              <div v-if="messages.length === 0" class="chat-empty">暂无消息，发一句试试吧</div>
              <div
                v-for="m in messages"
                :key="`${m.type}-${m.bizId}-${m.id}`"
                class="msg-row"
                :class="{ me: isMe(m) }"
              >
                <!-- 对方消息：头像在左，气泡在右 -->
                <template v-if="!isMe(m)">
                  <img
                    :src="activeConv.otherAvatarUrl || defaultAvatar"
                    class="bubble-avatar"
                    alt="other"
                  />
                  <div class="bubble">
                    <div class="bubble-text">{{ m.content }}</div>
                    <div class="bubble-time">{{ formatTime(m.createTime) }}</div>
                  </div>
                </template>

                <!-- 自己消息：气泡在左，头像在右 -->
                <template v-else>
                  <div class="bubble">
                    <div class="bubble-text">{{ m.content }}</div>
                    <div class="bubble-time">{{ formatTime(m.createTime) }}</div>
                  </div>
                  <img
                    :src="currentUser.avatarUrl || defaultAvatar"
                    class="bubble-avatar"
                    alt="me"
                  />
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input" v-if="activeConv">
          <el-input
            ref="chatInputRef"
            v-model="inputText"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入消息，Enter 发送（Shift+Enter 换行）"
            @keydown.enter.exact="handleEnterSend"
            @compositionstart="onCompositionStart"
            @compositionend="onCompositionEnd"
          />
          <div class="chat-actions">
            <div class="hint">
              {{ activeConv.type === 'TASK' ? '任务消息' : '商品交易' }} · {{ activeConv.title }}
            </div>
            <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.msg-shell {
  height: 100vh;
  background: radial-gradient(1200px 520px at 10% 0%, rgba(99, 102, 241, 0.10), transparent 60%),
    radial-gradient(900px 420px at 95% 20%, rgba(56, 189, 248, 0.10), transparent 55%),
    linear-gradient(180deg, #f8fafc, #f1f5f9);
  color: #0f172a;
}

.msg-nav-bar {
  padding: 10px 18px 4px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
}

.msg-nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.msg-nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-nav-logo {
  font-size: 18px;
  color: #2563eb;
}

.msg-nav-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.msg-nav-menu {
  border-bottom: none;
  flex: 1;
  display: flex;
  justify-content: center;
}

.msg-nav-right {
  display: flex;
  align-items: center;
  margin-left: 12px;
}

.nav-item-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.nav-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.msg-nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s ease;
}

.msg-nav-user:hover {
  background: rgba(37, 99, 235, 0.08);
}

.msg-nav-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-nav-avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  object-fit: cover;
  border: 2px solid rgba(37, 99, 235, 0.3);
}

.msg-topbar {
  height: 58px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 0 14px;
  gap: 10px;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(16px);
}

.back-btn {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  place-items: center;
  cursor: pointer;
}

.topbar-title {
  min-width: 0;
}
.topbar-main {
  font-weight: 800;
  letter-spacing: 0.02em;
}
.topbar-sub {
  font-size: 12px;
  color: #64748b;
}

.msg-frame {
  height: calc(100vh - 58px - 48px - 6px);
  display: grid;
  grid-template-columns: 360px 1fr;
  margin-top: 6px;
}

.sidebar {
  border-right: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
}

.sidebar-tabs {
  display: flex;
  flex-wrap: nowrap;
  justify-content: space-between;
  gap: 8px;
  padding: 12px;
}
.tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.8);
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}
.tab.active {
  background: rgba(37, 99, 235, 0.10);
  border-color: rgba(37, 99, 235, 0.22);
  color: #1d4ed8;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tab-badge {
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  line-height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sidebar-search {
  padding: 0 12px 12px;
}

.sidebar-list {
  flex: 1;
  overflow: auto;
  padding: 6px 8px 10px;
}

.list-loading {
  padding: 16px;
  color: #64748b;
  font-size: 13px;
}
.list-empty {
  padding: 26px 16px;
  text-align: center;
  color: #64748b;
}
.empty-icon {
  font-size: 28px;
  color: #94a3b8;
}
.empty-title {
  margin-top: 10px;
  font-weight: 800;
  color: #334155;
}
.empty-sub {
  margin-top: 4px;
  font-size: 12px;
}

.conv-item {
  width: 100%;
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 14px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.conv-item:hover {
  background: rgba(248, 250, 252, 0.8);
  border-color: rgba(226, 232, 240, 0.9);
}

.conv-item-ai {
  border-color: rgba(139, 92, 246, 0.25);
  background: rgba(139, 92, 246, 0.04);
}

.conv-item-ai:hover {
  background: rgba(139, 92, 246, 0.08);
  border-color: rgba(139, 92, 246, 0.35);
}
.conv-item.active {
  background: rgba(37, 99, 235, 0.08);
  border-color: rgba(37, 99, 235, 0.22);
}
.conv-avatar {
  position: relative;
  width: 44px;
  height: 44px;
}
.conv-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.10);
}
.badge {
  position: absolute;
  right: -6px;
  top: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 800;
  display: grid;
  place-items: center;
  border: 2px solid rgba(255, 255, 255, 0.9);
}
.conv-main {
  min-width: 0;
}
.conv-title-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: baseline;
}
.conv-title {
  font-weight: 900;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 响应式适配：窄屏下顶部导航和布局调整 */
@media (max-width: 900px) {
  .msg-nav-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .msg-nav-menu {
    justify-content: flex-start;
    margin-left: 0;
    overflow-x: auto;
  }

  .msg-frame {
    grid-template-columns: 1fr;
    height: calc(100vh - 58px - 48px - 6px);
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  }
}
.conv-time {
  font-size: 11px;
  color: #94a3b8;
}
.conv-sub-row {
  margin-top: 6px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
}
.conv-sub {
  font-size: 12px;
  color: #64748b;
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-last {
  font-size: 12px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
}

.chat-head {
  padding: 14px 18px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.chat-head-title {
  font-weight: 900;
  font-size: 15px;
  color: #0f172a;
}
.chat-head-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}
.chat-head-clear {
  font-size: 12px;
  color: #ef4444;
}

.chat-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}
.chat-placeholder {
  height: 100%;
  display: grid;
  place-items: center;
  padding: 18px;
}
.placeholder-card {
  width: min(520px, 92%);
  padding: 22px 22px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.10);
}
.placeholder-title {
  font-weight: 900;
  color: #0f172a;
}
.placeholder-sub {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
}

.chat-scroll {
  height: 100%;
  overflow: auto;
  padding: 18px 18px 10px;
}
.chat-loading,
.chat-empty {
  color: #64748b;
  font-size: 13px;
  text-align: center;
  padding: 18px 0;
}

.msg-row {
  display: flex;
  flex-direction: row;
  gap: 10px;
  margin: 12px 0;
  align-items: flex-end;
  justify-content: flex-start;
}
.msg-row.me {
  justify-content: flex-end;
}
.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}
.bubble {
  max-width: 620px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}
.msg-row.me .bubble {
  background: rgba(37, 99, 235, 0.10);
  border-color: rgba(37, 99, 235, 0.18);
}
.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  color: #0f172a;
  line-height: 1.55;
}
.bubble-time {
  margin-top: 6px;
  font-size: 11px;
  color: #94a3b8;
}

.chat-input {
  padding: 12px 14px 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
}
.chat-actions {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.hint {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .msg-frame {
    grid-template-columns: 320px 1fr;
  }
}
@media (max-width: 820px) {
  .msg-frame {
    grid-template-columns: 1fr;
  }
  .sidebar {
    height: 46vh;
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  }
}
</style>

