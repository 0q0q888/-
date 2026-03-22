<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  EditPen,
  ShoppingBag,
  Message,
  ChatDotRound,
  School,
  ArrowDown,
} from '@element-plus/icons-vue'
import axios from 'axios'

/** 默认头像（未设置或加载失败时使用） */
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

/**
 * 当前登录用户的基础信息（用于顶部导航显示）。
 */
const currentUser = ref({
  id: null,
  nickname: '未登录用户',
  gender: 0,
  role: 'USER',
  avatarUrl: defaultAvatar,
})

/**
 * 从后端拉取最新用户资料，用于正确显示头像和昵称。
 */
const refreshNavUser = async () => {
  const id = currentUser.value?.id
  if (!id) return
  try {
    const { data } = await axios.get(`/api/users/${id}`)
    if (data) {
      currentUser.value.nickname = data.nickname || data.username || currentUser.value.nickname
      currentUser.value.avatarUrl = data.avatarUrl || defaultAvatar
      currentUser.value.gender = data.gender ?? 0
    }
  } catch (e) {
    // 静默失败，保留 localStorage 数据
  }
}

onMounted(() => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      currentUser.value = {
        id: parsed.id ?? null,
        nickname: parsed.nickname || parsed.username || '未命名用户',
        gender: parsed.gender ?? 0,
        role: parsed.role || 'USER',
        avatarUrl: parsed.avatarUrl || defaultAvatar,
      }
      refreshNavUser()
    }
  } catch (e) {
    // 解析失败时忽略
  }
})

/**
 * 顶部导航栏的当前激活菜单 key。
 * 说明：
 * - tasks: 任务大厅
 * - publish-task: 发布任务
 * - market: 交易市场
 * - profile: 个人中心
 */
const router = useRouter()

const activeNav = ref('tasks')

/**
 * 顶部导航栏菜单配置。
 * 后续如果接入路由，可以在 onClick 里根据 key 跳转到不同页面。
 */
const navItems = [
  { key: 'tasks', label: '任务大厅' },
  { key: 'publish-task', label: '发布任务' },
  { key: 'market', label: '交易市场' },
  { key: 'messages', label: '我的消息' },
  { key: 'profile', label: '个人中心' },
]

/** 全局未读消息总数（任务 + 商品），用于顶部“我的消息”红点提示 */
const unreadTotal = ref(0)
let unreadTimer = null

/**
 * 顶部导航点击事件处理。
 * 现在仅切换激活态，后续可以在这里接入路由或弹窗。
 *
 * @param {string} key 导航项的 key
 */
const handleNavClick = (key) => {
  activeNav.value = key
  if (key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (key === 'publish-task') {
    router.push('/tasks?publish=1')
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
    return
  }
}

/**
 * 处理头像区域下拉菜单的指令。
 *
 * @param {string} command 下拉项的命令标识
 */
const handleUserCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('currentUser')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

/**
 * 计算属性：性别符号（红蓝图标）。
 * 说明：
 * - 1：男，用蓝色 ♂；
 * - 2：女，用红色 ♀；
 * - 其他：返回空字符串，不展示。
 */
const genderSymbol = computed(() => {
  if (currentUser.value.gender === 1) return '♂'
  if (currentUser.value.gender === 2) return '♀'
  return ''
})

/** 拉取当前用户的未读消息总数（任务 + 商品） */
const refreshUnreadTotal = async () => {
  if (!currentUser.value.id) {
    unreadTotal.value = 0
    return
  }
  try {
    const { data } = await axios.get('/api/messages/unread-total', {
      params: { userId: currentUser.value.id },
    })
    unreadTotal.value = Number.isFinite(Number(data)) ? Number(data) : 0
  } catch {
    // 静默失败，不影响主流程
  }
}

onMounted(() => {
  // 原有逻辑已经在文件前面处理 currentUserId，这里只负责拉未读数
  refreshUnreadTotal()
  unreadTimer = setInterval(refreshUnreadTotal, 1000)
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})
</script>

<template>
  <div class="home-page">
    <!-- 顶部导航栏：系统名称 + 中间导航 + 右侧用户信息 -->
    <header class="top-nav">
      <div class="brand">
        <el-icon class="brand-logo"><School /></el-icon>
        <div class="brand-text">
          <div class="brand-title">校园即时服务系统</div>
          <div class="brand-subtitle">Campus Instant Service Platform</div>
        </div>
      </div>

      <nav class="nav-center">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          class="nav-item"
          :class="{ active: activeNav === item.key }"
          @click="handleNavClick(item.key)"
        >
          <span class="nav-item-label">
            {{ item.label }}
            <span
              v-if="item.key === 'messages' && unreadTotal > 0"
              class="nav-badge"
            >
              {{ unreadTotal }}
            </span>
          </span>
        </button>
      </nav>

      <div class="nav-right">
        <div class="nav-user" @click="router.push('/profile')">
          <div class="user-info">
            <div class="user-name">
              {{ currentUser.nickname }}
              <span
                v-if="genderSymbol"
                class="gender-icon"
                :class="{ male: currentUser.gender === 1, female: currentUser.gender === 2 }"
              >
                {{ genderSymbol }}
              </span>
            </div>
          </div>
          <div class="user-avatar-wrapper">
            <img class="user-avatar" :src="currentUser.avatarUrl" alt="用户头像" />
          </div>
        </div>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button type="button" class="nav-more-btn" aria-label="更多">
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 欢迎 / 落地页主体 -->
    <main class="home-main">
      <!-- Hero：左侧品牌故事 + 右侧功能概览，参考常见 SaaS 官网布局 -->
      <section class="hero">
        <div class="hero-left">
          <div class="hero-badge">
            <span class="hero-badge-dot" />
            <span class="hero-badge-text">
              {{ currentUser.role === 'ADMIN' ? '管理员控制台' : '校园服务 · 一站集成' }}
            </span>
          </div>
          <h1 class="hero-title">
            欢迎来到
            <span class="hero-highlight">校园即时服务平台</span>
          </h1>
          <p class="hero-subtitle">
            一个整合
            <strong>任务跑腿</strong>、
            <strong>二手交易</strong>、
            <strong>即时消息</strong>
            的校园服务系统，让同学之间的协作与互助更加高效、安全、可信。
          </p>

          <div class="hero-actions">
            <el-button
              type="primary"
              size="large"
              round
              @click="handleNavClick('tasks')"
            >
              <el-icon style="margin-right: 6px"><EditPen /></el-icon>
              进入任务大厅
            </el-button>
            <el-button
              size="large"
              round
              @click="handleNavClick('market')"
            >
              <el-icon style="margin-right: 6px"><ShoppingBag /></el-icon>
              浏览二手交易
            </el-button>
          </div>

          <div class="hero-secondary-actions">
            <button
              type="button"
              class="link-btn"
              @click="handleNavClick('publish-task')"
            >
              立刻发布一个任务
            </button>
            <span class="divider">·</span>
            <button
              type="button"
              class="link-btn"
              @click="handleNavClick('messages')"
            >
              查看我的消息
            </button>
          </div>

          <div class="hero-user-summary">
            <div class="hero-user-main">
              <img
                :src="currentUser.avatarUrl"
                alt="avatar"
                class="hero-user-avatar"
              />
              <div class="hero-user-info">
                <div class="hero-user-name">
                  {{ currentUser.nickname }}
                </div>
                <div class="hero-user-role">
                  {{ currentUser.role === 'ADMIN' ? '平台管理员' : '校园用户' }}
                </div>
              </div>
            </div>
            <div class="hero-user-hint">
              完善个人资料并绑定钱包，可享受完整的任务结算与交易体验。
            </div>
          </div>
        </div>

        <!-- 右侧：用 Element Plus 卡片组件做功能模块展示，更贴近商业官网样式 -->
        <div class="hero-right">
          <el-card class="hero-card hero-card-primary" shadow="always">
            <template #header>
              <div class="hero-card-header">
                <div class="hero-card-title">
                  <el-icon class="hero-card-icon"><EditPen /></el-icon>
                  任务 &amp; 跑腿服务
                </div>
                <el-tag size="small" type="primary" effect="plain">即时撮合</el-tag>
              </div>
            </template>
            <ul class="hero-card-list">
              <li>支持完整的任务信息配置与筛选，适配不同场景的校园需求</li>
              <li>通过钱包托管与任务状态流转，降低线下协作的信任成本</li>
              <li>你可以像使用专业众包平台一样，高效管理个人任务</li>
            </ul>
          </el-card>

          <el-card class="hero-card hero-card-secondary" shadow="always">
            <template #header>
              <div class="hero-card-title">
                <el-icon class="hero-card-icon"><ShoppingBag /></el-icon>
                二手交易市场
              </div>
            </template>
            <ul class="hero-card-list">
              <li>支持本地图片上传、搜索、我的商品等核心交易闭环</li>
              <li>买卖双方可通过「我想要 / 私聊我」快速发起沟通</li>
              <li>交易消息与订单记录统一归档，类似专业电商后台</li>
            </ul>
          </el-card>

          <el-card class="hero-card hero-card-tertiary" shadow="always">
            <template #header>
              <div class="hero-card-title">
                <el-icon class="hero-card-icon"><Message /></el-icon>
                实时消息与个人中心
              </div>
            </template>
            <ul class="hero-card-list">
              <li>任务 / 商品交易分频道展示，聊天布局参考桌面版微信</li>
              <li>WebSocket 实时推送消息，自动滚动定位到最新一条</li>
              <li>个人中心提供头像裁剪、资料完善与钱包流水明细</li>
            </ul>
          </el-card>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 40px;
  position: relative;
  /* 背景使用多重渐变，营造商业化的氛围感 */
  background:
    radial-gradient(1200px 520px at 10% 0%, rgba(59, 130, 246, 0.10), transparent 60%),
    radial-gradient(900px 420px at 96% 10%, rgba(56, 189, 248, 0.10), transparent 55%),
    linear-gradient(180deg, #f8fafc, #eef2ff);
}

.top-nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  column-gap: 24px;
  padding: 12px 18px;
  border-radius: 20px;
  background: #f3f4f6;
  box-shadow:
    0 12px 30px rgba(15, 23, 42, 0.08),
    0 0 0 1px rgba(226, 232, 240, 0.9);
  margin-bottom: 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 20px;
  color: #0b1220;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(129, 140, 248, 0.18));
}
.brand-logo svg {
  width: 1.25em;
  height: 1.25em;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 0.12em;
  color: #0f172a;
}

.brand-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: #6b7280;
}

.nav-center {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.nav-item {
  border: none;
  background: transparent;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
  transition:
    background-color 160ms ease,
    color 160ms ease,
    transform 140ms ease;
}

.nav-item:hover {
  background: rgba(148, 163, 184, 0.12);
  transform: translateY(-1px);
}

.nav-item.active {
  background: #2563eb;
  color: #f9fafb;
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

.nav-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s ease;
}

.nav-user:hover {
  background: rgba(148, 163, 184, 0.12);
}

.nav-more-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: background 0.2s ease, color 0.2s ease;
}

.nav-more-btn:hover {
  background: rgba(148, 163, 184, 0.12);
  color: #111827;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.gender-icon {
  margin-left: 4px;
  font-size: 12px;
}

.gender-icon.male {
  color: #2563eb;
}

.gender-icon.female {
  color: #f97316;
}

.user-role {
  margin-top: 2px;
  font-size: 11px;
  color: #6b7280;
}

.user-avatar-wrapper {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  padding: 2px;
  background: linear-gradient(135deg, #93c5fd, #a5b4fc);
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
  background: #e5e7eb;
}

/* 欢迎页主体布局 */
.home-main {
  margin-top: 24px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  gap: 26px;
  align-items: center;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.06);
  color: #1d4ed8;
  font-size: 11px;
  width: fit-content;
}

.hero-badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.35);
}

.hero-badge-text {
  letter-spacing: 0.08em;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
  color: #020617;
}

.hero-highlight {
  background: linear-gradient(120deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #4b5563;
  max-width: 34rem;
}

.hero-subtitle strong {
  font-weight: 600;
  color: #111827;
}

.hero-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-secondary-actions {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.link-btn {
  border: none;
  padding: 0;
  background: none;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

.divider {
  color: #9ca3af;
}

.hero-user-summary {
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: 18px;
  border: 1px dashed rgba(148, 163, 184, 0.9);
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-user-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  object-fit: cover;
}

.hero-user-info {
  display: flex;
  flex-direction: column;
}

.hero-user-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.hero-user-role {
  font-size: 11px;
  color: #6b7280;
}

.hero-user-hint {
  font-size: 11px;
  color: #9ca3af;
}

.hero-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-card {
  border-radius: 18px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.hero-card-primary {
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.14), transparent 60%),
    #ffffff;
}

.hero-card-secondary {
  background:
    radial-gradient(circle at top right, rgba(52, 211, 153, 0.16), transparent 60%),
    #ffffff;
}

.hero-card-tertiary {
  background:
    radial-gradient(circle at bottom right, rgba(129, 140, 248, 0.18), transparent 60%),
    #ffffff;
}

.hero-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.hero-card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.hero-card-icon {
  font-size: 15px;
  color: #2563eb;
}

.hero-card-primary .hero-card-icon {
  color: #2563eb;
}
.hero-card-secondary .hero-card-icon {
  color: #0f766e;
}
.hero-card-tertiary .hero-card-icon {
  color: #7c3aed;
}

.hero-card-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.9);
}

.hero-card-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.7;
}

.hero-card-list li + li {
  margin-top: 2px;
}



@media (max-width: 900px) {
  .top-nav {
    grid-template-columns: 1fr;
    row-gap: 8px;
  }

  .nav-center {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .nav-user {
    justify-content: space-between;
  }

  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .floating-actions {
    right: 12px;
    top: auto;
    bottom: 18px;
    transform: none;
  }

  .floating-card {
    width: auto;
    flex-direction: row;
    border-radius: 999px;
  }

  .floating-item {
    padding: 6px 8px;
  }
}
</style>

