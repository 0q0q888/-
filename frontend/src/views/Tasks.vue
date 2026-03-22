<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search,
  Filter,
  Tickets,
  Timer,
  Location,
  School,
  ArrowDown,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

/**
 * 搜索关键字（任务标题 / 描述）
 */
const keyword = ref('')

/**
 * 当前筛选项（与数据库 t_task 字段对应的筛选条件）
 * - status: 任务状态（all / OPEN / TAKEN / FINISHED / CANCELLED）
 * - reward: 酬劳区间（all / 0-5 / 5-10 / 10+）
 * - sort: 排序方式（latest / reward_desc / reward_asc）
 */
const filterState = ref({
  status: 'all',
  reward: 'all',
  sort: 'latest',
})

/** 筛选弹层显示与否 */
const filterVisible = ref(false)

/**
 * 任务列表（从后端 /api/tasks 拉取的真实数据）
 */
const tasks = ref([])

/** 列表加载中的标记 */
const loading = ref(false)

/** 详情弹窗显示与否 */
const detailVisible = ref(false)

/** 当前选中的任务详情 */
const currentTask = ref(null)

/** 当前登录用户ID（从 localStorage 读取） */
const currentUserId = ref(null)

/** 我的任务：当前子 tab（accepted / published） */
const myTasksTab = ref('accepted')
/** 我的任务抽屉/面板是否显示 */
const myTasksVisible = ref(false)
/** 我接的单 */
const myAcceptedTasks = ref([])
/** 我发布的订单 */
const myPublishedTasks = ref([])
/** 我的任务加载中 */
const myTasksLoading = ref(false)

/** 发布任务弹窗是否显示 */
const publishDialogVisible = ref(false)
/** 发布任务表单 */
const publishForm = ref({
  title: '',
  description: '',
  rewardAmount: null,
  deadlineTime: null,
})
/** 发布任务提交中 */
const publishLoading = ref(false)

/** 默认头像（未设置或加载失败时使用） */
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

/**
 * 当前登录用户的基础信息（用于右上角头像展示）。
 */
const currentUser = ref({
  id: null,
  nickname: '未登录用户',
  gender: 0,
  role: 'USER',
  avatarUrl: defaultAvatar,
})

/** 顶部“我的消息”未读总数（任务 + 商品） */
const unreadTotal = ref(0)
let unreadTimer = null

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

/**
 * 从后端拉取最新用户资料，用于正确显示头像和昵称。
 */
const refreshNavUser = async () => {
  const id = currentUserId.value
  if (!id) return
  try {
    const { data } = await axios.get(`/api/users/${id}`)
    if (data) {
      currentUser.value.nickname = data.nickname || data.username || currentUser.value.nickname
      currentUser.value.avatarUrl = data.avatarUrl || defaultAvatar
      currentUser.value.gender = data.gender ?? 0
    }
  } catch (e) {
    // 静默失败
  }
}

/**
 * 计算属性：性别符号（红蓝图标）。
 */
const genderSymbol = computed(() => {
  if (currentUser.value.gender === 1) return '♂'
  if (currentUser.value.gender === 2) return '♀'
  return ''
})

/**
 * 点击顶部导航时的处理（与 Home 页保持一致）
 *
 * @param {string} key 导航项的 key
 */
const handleNavClick = (key) => {
  if (key === 'tasks') {
    // 已在任务大厅，无需跳转
    return
  }
  if (key === 'publish-task') {
    if (!currentUserId.value) {
      ElMessage.warning('请先登录后再发布任务')
      return
    }
    openPublishDialog()
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
 * 处理头像区域下拉菜单指令（退出登录等）。
 *
 * @param {string} command 下拉项命令
 */
const handleUserCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('currentUser')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

/**
 * 执行搜索的函数（当前仅在控制台输出，预留给后续接口）
 */
const handleSearch = () => {
  // 这里只做前端过滤，后续可将 keyword 作为查询参数传给后端
}

/**
 * 将当前筛选重置为默认值。
 */
const resetFilters = () => {
  filterState.value = {
    status: 'all',
    reward: 'all',
    sort: 'latest',
  }
}

/**
 * 应用筛选（目前只是关闭弹层 + 触发本地过滤）。
 */
const applyFilters = () => {
  filterVisible.value = false
}

/**
 * 根据关键字和筛选条件，计算过滤后的任务列表。
 * 说明：
 * - 关键字在 title 中做简单包含匹配；
 * - 状态根据 filterState.status 过滤；
 * - 酬劳区间根据 reward 值过滤；
 * - 排序根据 sort 做前端排序（示例）。
 */
const filteredTasks = computed(() => {
  let list = tasks.value.slice()

  // 关键字过滤
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter((item) =>
      item.title.toLowerCase().includes(kw),
    )
  }

  // 状态过滤
  if (filterState.value.status !== 'all') {
    list = list.filter((item) => item.status === filterState.value.status)
  }

  // 酬劳区间过滤
  if (filterState.value.reward === '0-5') {
    list = list.filter((item) => item.rewardAmount <= 5)
  } else if (filterState.value.reward === '5-10') {
    list = list.filter((item) => item.rewardAmount > 5 && item.rewardAmount <= 10)
  } else if (filterState.value.reward === '10+') {
    list = list.filter((item) => item.rewardAmount > 10)
  }

  // 排序
  if (filterState.value.sort === 'reward_desc') {
    list.sort((a, b) => b.rewardAmount - a.rewardAmount)
  } else if (filterState.value.sort === 'reward_asc') {
    list.sort((a, b) => a.rewardAmount - b.rewardAmount)
  }
  // latest 暂时使用原始顺序

  return list
})

/**
 * 统计信息：用于顶部概览卡片展示，增加页面的“信息密度感”。
 */
const totalTasks = computed(() => tasks.value.length)
const openTasks = computed(
  () => tasks.value.filter((item) => item.status === 'OPEN').length,
)
const takenTasks = computed(
  () => tasks.value.filter((item) => item.status === 'TAKEN').length,
)
const finishedTasks = computed(
  () => tasks.value.filter((item) => item.status === 'FINISHED').length,
)

/**
 * 从后端加载任务列表。
 * 说明：
 * - GET /api/tasks
 * - 仅做简单错误提示，不中断页面。
 */
const loadTasks = async () => {
  loading.value = true
  try {
    const resp = await axios.get('/api/tasks')
    tasks.value = Array.isArray(resp.data) ? resp.data : []
  } catch (error) {
    const msg = error?.response?.data || error?.message || '加载任务列表失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

/**
 * 打开任务详情弹窗。
 * 说明：
 * - 这里演示调用 GET /api/tasks/{id}，用于未来扩展更详细字段。
 *
 * @param {number} id 任务ID
 */
const openTaskDetail = async (id) => {
  try {
    const resp = await axios.get(`/api/tasks/${id}`)
    currentTask.value = resp.data
    detailVisible.value = true
  } catch (error) {
    const msg = error?.response?.data || error?.message || '加载任务详情失败'
    ElMessage.error(msg)
  }
}

/**
 * 是否可接单：状态为 OPEN 且未过截止时间。
 */
const canAccept = (task) => {
  if (!task || task.status !== 'OPEN') return false
  if (!task.deadlineTime) return true
  const end = parseDate(task.deadlineTime)
  return end && !isNaN(end.getTime()) && end > new Date()
}

/**
 * 格式化截止时间显示。
 */
const formatDeadline = (deadlineTime) => {
  if (deadlineTime == null) return '无'
  const d = parseDate(deadlineTime)
  if (!d || isNaN(d.getTime())) return '无'
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * 解析后端返回的日期（支持 ISO 字符串、时间戳、数组等）。
 */
const parseDate = (v) => {
  if (v == null) return null
  if (v instanceof Date) return v
  if (typeof v === 'number') return new Date(v)
  if (Array.isArray(v)) return new Date(v[0], (v[1] || 1) - 1, v[2] || 1, v[3] || 0, v[4] || 0, v[5] || 0)
  const s = String(v).trim()
  if (!s) return null
  return new Date(s.replace(' ', 'T'))
}

/**
 * 剩余时间文案（用于接单大厅和我的接单）。
 * 无截止时间或解析失败时显示「未设置」。
 */
const getRemainingTime = (deadlineTime) => {
  if (deadlineTime == null) return '未设置'
  const end = parseDate(deadlineTime)
  if (!end || isNaN(end.getTime())) return '未设置'
  const now = new Date()
  if (now >= end) return '已过期'
  const ms = end - now
  const days = Math.floor(ms / (24 * 60 * 60 * 1000))
  const hours = Math.floor((ms % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000))
  const mins = Math.floor((ms % (60 * 60 * 1000)) / (60 * 1000))
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${mins}分钟`
  return `${mins}分钟`
}

/**
 * 接单操作：当前登录用户为某个任务接单。
 *
 * @param {object} task 任务对象
 */
const handleTakeTask = async (task) => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录后再接单')
    return
  }
  if (!canAccept(task)) {
    ElMessage.info(task.status !== 'OPEN' ? '该任务当前不可接单' : '已过接单截止时间')
    return
  }
  try {
    await axios.post(`/api/tasks/${task.id}/take`, null, {
      params: { receiverId: currentUserId.value },
    })
    ElMessage.success('接单成功')
    await loadTasks()
    if (myTasksVisible.value) {
      await loadMyAccepted()
      await loadMyPublished()
    }
  } catch (error) {
    const msg = error?.response?.data || error?.message || '接单失败'
    ElMessage.error(msg)
  }
}

/**
 * 当前任务是否是我发布的。
 *
 * @param {object} task 任务对象
 * @returns {boolean} true 表示 task.publisherId 等于当前用户ID
 */
const isMyPublishedTask = (task) => {
  if (!task || !currentUserId.value) return false
  return String(task.publisherId) === String(currentUserId.value)
}

/**
 * 当前任务是否是我接下的。
 *
 * @param {object} task 任务对象
 * @returns {boolean} true 表示 task.receiverId 等于当前用户ID
 */
const isMyAcceptedTask = (task) => {
  if (!task || !currentUserId.value || task.receiverId == null) return false
  return String(task.receiverId) === String(currentUserId.value)
}

/** 加载我接的单 */
const loadMyAccepted = async () => {
  if (!currentUserId.value) return
  myTasksLoading.value = true
  try {
    const { data } = await axios.get('/api/tasks/my-accepted', {
      params: { userId: currentUserId.value },
    })
    myAcceptedTasks.value = Array.isArray(data) ? data : []
  } catch (e) {
    myAcceptedTasks.value = []
  } finally {
    myTasksLoading.value = false
  }
}

/** 加载我发布的订单 */
const loadMyPublished = async () => {
  if (!currentUserId.value) return
  myTasksLoading.value = true
  try {
    const { data } = await axios.get('/api/tasks/my-published', {
      params: { userId: currentUserId.value },
    })
    myPublishedTasks.value = Array.isArray(data) ? data : []
  } catch (e) {
    myPublishedTasks.value = []
  } finally {
    myTasksLoading.value = false
  }
}

/** 打开我的任务面板并加载数据 */
const openMyTasks = () => {
  myTasksVisible.value = true
  myTasksTab.value = 'accepted'
  loadMyAccepted()
  loadMyPublished()
}

/**
 * 发布人确认任务完成，酬劳转给接单人。
 */
/** 打开发布任务弹窗 */
const openPublishDialog = () => {
  publishForm.value = {
    title: '',
    description: '',
    rewardAmount: null,
    deadlineTime: null,
  }
  publishDialogVisible.value = true
}

/** 关闭发布任务弹窗 */
const closePublishDialog = () => {
  publishDialogVisible.value = false
}

/** 提交发布任务 */
const submitPublishTask = async () => {
  const { title, description, rewardAmount, deadlineTime } = publishForm.value
  if (!title || !title.trim()) {
    ElMessage.warning('请填写任务标题')
    return
  }
  if (!description || !description.trim()) {
    ElMessage.warning('请填写任务描述')
    return
  }
  if (rewardAmount == null || rewardAmount < 0) {
    ElMessage.warning('请填写有效报酬（≥0）')
    return
  }
  if (!deadlineTime) {
    ElMessage.warning('请选择截止时间')
    return
  }
  const d = deadlineTime instanceof Date ? deadlineTime : new Date(deadlineTime)
  const pad = (n) => String(n).padStart(2, '0')
  const deadline = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
  if (d <= new Date()) {
    ElMessage.warning('截止时间必须晚于当前时间')
    return
  }
  publishLoading.value = true
  try {
    await axios.post(
      '/api/tasks',
      {
        title: title.trim(),
        description: description.trim(),
        rewardAmount: Number(rewardAmount),
        deadlineTime: deadline,
      },
      { params: { publisherId: currentUserId.value } }
    )
    ElMessage.success('发布成功')
    closePublishDialog()
    await loadTasks()
    await loadMyPublished()
  } catch (error) {
    const msg = error?.response?.data?.message || error?.response?.data || error?.message || '发布失败'
    ElMessage.error(typeof msg === 'string' ? msg : JSON.stringify(msg))
  } finally {
    publishLoading.value = false
  }
}

const handleCompleteTask = async (task) => {
  if (!currentUserId.value || task.publisherId !== currentUserId.value) {
    ElMessage.warning('仅发布人可确认完成')
    return
  }
  if (task.status !== 'TAKEN') {
    ElMessage.info('当前任务状态不可确认完成')
    return
  }
  try {
    await axios.post(`/api/tasks/${task.id}/complete`, null, {
      params: { publisherId: currentUserId.value },
    })
    ElMessage.success('已确认完成，酬劳已转给接单人')
    await loadTasks()
    await loadMyPublished()
    await loadMyAccepted()
  } catch (error) {
    const msg = error?.response?.data || error?.message || '操作失败'
    ElMessage.error(msg)
  }
}

onMounted(() => {
  const raw = window.localStorage.getItem('currentUser')
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
        currentUserId.value = parsed.id ?? null
        currentUser.value = {
          id: parsed.id ?? null,
          nickname: parsed.nickname || parsed.username || '未命名用户',
          gender: parsed.gender ?? 0,
          role: parsed.role || 'USER',
          avatarUrl: parsed.avatarUrl || defaultAvatar,
        }
        refreshNavUser()
        refreshUnreadTotal()
      }
    } catch (e) {
      // 忽略解析错误
    }
  }
  loadTasks()
  // 从首页/右侧悬浮栏带 ?publish=1 进入，或从 sessionStorage 得知需要打开发布弹窗（因 key=fullPath 会重建组件，用标记保留意图）
  if (route.query.publish === '1') {
    if (currentUserId.value) {
      try {
        sessionStorage.setItem('openPublishDialog', '1')
      } catch (_) {}
    }
    router.replace({ path: '/tasks', query: {} })
  }
  try {
    if (sessionStorage.getItem('openPublishDialog') === '1') {
      sessionStorage.removeItem('openPublishDialog')
      if (currentUserId.value) openPublishDialog()
      else ElMessage.warning('请先登录后再发布任务')
    }
  } catch (_) {}

  // 每秒刷新未读数，驱动顶部“我的消息”红点
  unreadTimer = setInterval(refreshUnreadTotal, 1000)
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})

/** 监听路由：已在任务页时点击右侧「发任务」会变成 ?publish=1，直接打开发布弹窗并清理 URL */
watch(
  () => route.query.publish,
  (publish) => {
    if (route.path !== '/tasks' || publish !== '1') return
    router.replace({ path: '/tasks', query: {} })
    if (currentUserId.value) openPublishDialog()
    else ElMessage.warning('请先登录后再发布任务')
  }
)
</script>

<template>
  <div class="tasks-page">
    <!-- 顶部导航（与 Home 保持风格统一） -->
    <header class="top-nav">
      <div class="brand">
        <el-icon class="brand-logo"><School /></el-icon>
        <div class="brand-text">
          <div class="brand-title">校园即时服务系统</div>
          <div class="brand-subtitle">任务大厅 · Tasks</div>
        </div>
      </div>

      <nav class="nav-center">
        <button type="button" class="nav-item active">
          任务大厅
        </button>
        <button type="button" class="nav-item" @click="handleNavClick('publish-task')">
          发布任务
        </button>
        <button type="button" class="nav-item" @click="handleNavClick('market')">
          交易市场
        </button>
        <button type="button" class="nav-item" @click="handleNavClick('messages')">
          <span class="nav-item-label">
            我的消息
            <span
              v-if="unreadTotal > 0"
              class="nav-badge"
            >
              {{ unreadTotal }}
            </span>
          </span>
        </button>
        <button type="button" class="nav-item" @click="handleNavClick('profile')">
          个人中心
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

    <!-- 搜索 + 筛选区域 -->
    <section class="search-section">
      <el-input
        v-model="keyword"
        size="large"
        class="search-input"
        placeholder="搜索任务关键字，例如：取快递 / 打印资料"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="search-actions">
        <el-button
          size="large"
          type="primary"
          @click="handleSearch"
        >
          <el-icon style="margin-right: 4px">
            <Search />
          </el-icon>
          搜索
        </el-button>
        <el-button
          size="large"
          class="filter-button"
          @click="openMyTasks"
        >
          <el-icon style="margin-right: 4px">
            <Tickets />
          </el-icon>
          我的任务
        </el-button>
        <el-popover
          v-model:visible="filterVisible"
          placement="bottom-end"
          width="260"
          trigger="click"
        >
          <template #reference>
            <el-button
              size="large"
              class="filter-button"
            >
              <el-icon style="margin-right: 4px">
                <Filter />
              </el-icon>
              筛选
            </el-button>
          </template>

          <div class="filter-panel">
            <div class="filter-group">
              <div class="filter-label">任务状态</div>
              <el-radio-group v-model="filterState.status" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="OPEN">可接单</el-radio-button>
                <el-radio-button value="TAKEN">已被接</el-radio-button>
                <el-radio-button value="FINISHED">已完成</el-radio-button>
              </el-radio-group>
            </div>

            <div class="filter-group">
              <div class="filter-label">酬劳区间（元）</div>
              <el-radio-group v-model="filterState.reward" size="small">
                <el-radio-button value="all">不限</el-radio-button>
                <el-radio-button value="0-5">0 - 5</el-radio-button>
                <el-radio-button value="5-10">5 - 10</el-radio-button>
                <el-radio-button value="10+">10 以上</el-radio-button>
              </el-radio-group>
            </div>

            <div class="filter-group">
              <div class="filter-label">排序方式</div>
              <el-radio-group v-model="filterState.sort" size="small">
                <el-radio-button value="latest">最新发布</el-radio-button>
                <el-radio-button value="reward_desc">酬劳最高</el-radio-button>
                <el-radio-button value="reward_asc">酬劳最低</el-radio-button>
              </el-radio-group>
            </div>

            <div class="filter-footer">
              <el-button size="small" @click="resetFilters">重置</el-button>
              <el-button
                size="small"
                type="primary"
                @click="applyFilters"
              >
                应用
              </el-button>
            </div>
          </div>
        </el-popover>
      </div>
    </section>

    <!-- 任务列表骨架 -->
    <section class="tasks-section">
      <div class="tasks-header">
        <div class="tasks-header-main">
          <div>
            <h2 class="tasks-title">正在进行的任务</h2>
            <p class="tasks-subtitle">为你推荐校园内最新、报酬合理的校园任务</p>
          </div>
          <div class="tasks-badge">
            今日可接单
            <span class="tasks-badge-number">{{ openTasks }}</span>
          </div>
        </div>
        <div class="tasks-metrics">
          <div class="metric-card">
            <div class="metric-label">全部任务</div>
            <div class="metric-value">{{ totalTasks }}</div>
            <div class="metric-hint">当前列表</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">可接单</div>
            <div class="metric-value metric-value-green">{{ openTasks }}</div>
            <div class="metric-hint">状态为 OPEN</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">进行中</div>
            <div class="metric-value metric-value-blue">{{ takenTasks }}</div>
            <div class="metric-hint">已被同学接单</div>
          </div>
          <div class="metric-card metric-card-muted">
            <div class="metric-label">已完成</div>
            <div class="metric-value">{{ finishedTasks }}</div>
            <div class="metric-hint">历史成交</div>
          </div>
        </div>
      </div>

      <el-skeleton
        v-if="loading"
        animated
        :rows="3"
      />
      <template v-else>
        <el-empty
          v-if="filteredTasks.length === 0"
          description="暂无任务，稍后再来看看~"
        />
        <el-row
          v-else
          :gutter="14"
        >
          <el-col
            v-for="item in filteredTasks"
            :key="item.id"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <el-card class="task-card" shadow="hover">
              <div class="task-tag">
                <el-icon class="task-tag-icon">
                  <Tickets />
                </el-icon>
                <span class="task-tag-text">
                  {{ item.status === 'OPEN' ? '可接单' : item.status === 'TAKEN' ? '进行中' : item.status === 'FINISHED' ? '已完成' : '已取消' }}
                </span>
              </div>
              <div class="task-title">
                {{ item.title }}
              </div>
              <div class="task-meta">
                <div class="task-meta-item">
                  <el-icon><Location /></el-icon>
                  <span>发布人：{{ item.publisherNickname || '未知' }}</span>
                </div>
                <!-- 如果是当前用户发布或接下的任务，在卡片上做出标记 -->
                <div
                  v-if="isMyPublishedTask(item)"
                  class="task-meta-item task-mine-badge"
                >
                  <el-tag size="small" type="warning" effect="plain">我发布的任务</el-tag>
                </div>
                <div
                  v-else-if="isMyAcceptedTask(item)"
                  class="task-meta-item task-mine-badge"
                >
                  <el-tag size="small" type="success" effect="plain">我接的任务</el-tag>
                </div>
                <div class="task-meta-item">
                  <el-icon><Timer /></el-icon>
                  <span>截止时间：{{ formatDeadline(item.deadlineTime) }}</span>
                </div>
                <div class="task-meta-item">
                  <span>创建：{{ item.createTime }}</span>
                </div>
              </div>
              <div class="task-footer">
                <div class="reward-row">
                  <div class="reward">
                    <span class="reward-label">酬劳</span>
                    <span class="reward-value">￥{{ (item.rewardAmount ?? 0).toFixed(2) }}</span>
                  </div>
                  <div class="remaining-time">
                    <el-icon><Timer /></el-icon>
                    <span>剩余 {{ getRemainingTime(item.deadlineTime) }}</span>
                  </div>
                </div>
                <div class="task-actions">
                  <el-button
                    v-if="item.status === 'OPEN' && !isMyPublishedTask(item) && !isMyAcceptedTask(item)"
                    type="primary"
                    size="small"
                    round
                    :disabled="!canAccept(item)"
                    @click="handleTakeTask(item)"
                  >
                    {{ canAccept(item) ? '接单' : '已截止' }}
                  </el-button>
                  <el-button
                    size="small"
                    round
                    @click="openTaskDetail(item.id)"
                  >
                    查看详情
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </section>

    <!-- 任务详情弹窗 -->
    <el-drawer
      v-model="detailVisible"
      title="任务详情"
      size="400px"
      direction="rtl"
    >
      <template v-if="currentTask">
        <h3 class="detail-title">{{ currentTask.title }}</h3>
        <p class="detail-desc">
          {{ currentTask.description }}
        </p>
        <el-descriptions
          column="1"
          size="small"
          border
        >
          <el-descriptions-item label="任务状态">
            {{ currentTask.status === 'OPEN' ? '可接单' : currentTask.status === 'TAKEN' ? '进行中' : currentTask.status === 'FINISHED' ? '已完成' : '已取消' }}
          </el-descriptions-item>
          <el-descriptions-item label="酬劳">
            ￥{{ (currentTask.rewardAmount ?? 0).toFixed(2) }}
          </el-descriptions-item>
          <el-descriptions-item label="接单截止时间">
            {{ formatDeadline(currentTask.deadlineTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="发布人">
            {{ currentTask.publisherNickname || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="接单人">
            {{ currentTask.receiverNickname || '暂无' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ currentTask.createTime }}
          </el-descriptions-item>
        </el-descriptions>
        <!-- 详情中：如果是我自己的任务（发布或接单），不再显示“接单”按钮 -->
        <div
          v-if="
            currentTask.status === 'OPEN' &&
              !isMyPublishedTask(currentTask) &&
              !isMyAcceptedTask(currentTask)
          "
          class="detail-actions"
        >
          <el-button
            type="primary"
            :disabled="!canAccept(currentTask)"
            @click="handleTakeTask(currentTask); detailVisible = false"
          >
            {{ canAccept(currentTask) ? '接单' : '已过截止时间' }}
          </el-button>
        </div>
      </template>
      <template v-else>
        <el-empty description="暂无任务详情" />
      </template>
    </el-drawer>

    <!-- 发布任务弹窗（欢迎页风格，带说明与分组） -->
    <el-dialog
      v-model="publishDialogVisible"
      width="560px"
      :close-on-click-modal="false"
      @close="closePublishDialog"
      class="publish-task-dialog"
    >
      <template #header>
        <div class="publish-task-header">
          <div class="publish-task-icon">
            <el-icon><EditPen /></el-icon>
          </div>
          <div class="publish-task-header-text">
            <div class="publish-task-title">发布新任务</div>
            <div class="publish-task-sub">
              填写清晰的标题、描述和合理的报酬，有助于任务更快被同学接单。
            </div>
          </div>
        </div>
      </template>
      <div class="publish-task-body">
        <div class="publish-task-main">
          <el-form :model="publishForm" label-position="top" class="publish-task-form">
            <div class="publish-task-section">
              <div class="section-label">基础信息</div>
              <el-form-item label="标题" required>
                <el-input
                  v-model="publishForm.title"
                  placeholder="例如：帮忙代取快递 / 借一支黑色笔记本电脑电源"
                  maxlength="100"
                  show-word-limit
                  clearable
                />
              </el-form-item>
              <el-form-item label="任务描述" required>
                <el-input
                  v-model="publishForm.description"
                  type="textarea"
                  placeholder="补充任务地点、时间要求、注意事项等信息"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </div>

            <div class="publish-task-section publish-task-section-grid">
              <div class="section-label">报酬与截止时间</div>
              <div class="section-grid">
                <el-form-item label="报酬金额（元）" required>
                  <el-input-number
                    v-model="publishForm.rewardAmount"
                    :min="0"
                    :precision="2"
                    :step="0.5"
                    placeholder="0.00"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="接单截止时间" required>
                  <el-date-picker
                    v-model="publishForm.deadlineTime"
                    type="datetime"
                    placeholder="选择截止日期与时间"
                    format="YYYY-MM-DD HH:mm"
                    style="width: 100%"
                    :disabled-date="(d) => d && d.getTime() < Date.now() - 60000"
                  />
                </el-form-item>
              </div>
            </div>
          </el-form>
        </div>
        <aside class="publish-task-aside">
          <div class="aside-title">小提示</div>
          <ul class="aside-list">
            <li>标题中包含<strong>地点 +时间 +简要需求</strong>，更容易被搜索到。</li>
            <li>描述中说明任务<strong>预期耗时、联系方式</strong>，方便同学评估。</li>
            <li>酬劳金额建议与任务难度匹配，避免过高或过低造成误解。</li>
          </ul>
        </aside>
      </div>
      <template #footer>
        <div class="publish-task-footer">
          <el-button @click="closePublishDialog">取消</el-button>
          <el-button type="primary" :loading="publishLoading" @click="submitPublishTask">
            发布任务
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 我的任务抽屉 -->
    <el-drawer
      v-model="myTasksVisible"
      title="我的任务"
      size="480px"
      direction="rtl"
    >
      <el-tabs v-model="myTasksTab">
        <el-tab-pane label="我的接单" name="accepted">
          <p class="my-tasks-hint">我接下的任务，展示详情与剩余时间</p>
          <el-skeleton v-if="myTasksLoading" :rows="3" animated />
          <template v-else>
            <el-empty v-if="myAcceptedTasks.length === 0" description="暂无接单" />
            <div v-else class="my-task-list">
              <div
                v-for="t in myAcceptedTasks"
                :key="t.id"
                class="my-task-item"
              >
                <div class="my-task-title">{{ t.title }}</div>
                <div class="my-task-meta">
                  <span>酬劳 ￥{{ (t.rewardAmount ?? 0).toFixed(2) }}</span>
                  <span>发布人：{{ t.publisherNickname || '未知' }}</span>
                </div>
                <div class="my-task-deadline">
                  <el-icon><Timer /></el-icon>
                  剩余时间：{{ getRemainingTime(t.deadlineTime) }}
                  <span v-if="t.deadlineTime" class="my-task-deadline-raw">（截止 {{ formatDeadline(t.deadlineTime) }}）</span>
                </div>
                <el-button size="small" text type="primary" @click="openTaskDetail(t.id)">查看详情</el-button>
              </div>
            </div>
          </template>
        </el-tab-pane>
        <el-tab-pane label="我的订单" name="published">
          <p class="my-tasks-hint">我发布的任务，可确认完成后把酬劳转给接单人；超时未完成将自动退回钱包</p>
          <el-skeleton v-if="myTasksLoading" :rows="3" animated />
          <template v-else>
            <el-empty v-if="myPublishedTasks.length === 0" description="暂无发布" />
            <div v-else class="my-task-list">
              <div
                v-for="t in myPublishedTasks"
                :key="t.id"
                class="my-task-item"
              >
                <div class="my-task-title">{{ t.title }}</div>
                <div class="my-task-meta">
                  <span>酬劳 ￥{{ (t.rewardAmount ?? 0).toFixed(2) }}</span>
                  <span>接单人：{{ t.receiverNickname || '暂无' }}</span>
                </div>
                <div class="my-task-status">
                  {{ t.status === 'OPEN' ? '待接单' : t.status === 'TAKEN' ? '进行中' : t.status === 'FINISHED' ? '已完成' : '已取消' }}
                </div>
                <div class="my-task-item-actions">
                  <el-button
                    v-if="t.status === 'TAKEN'"
                    type="primary"
                    size="small"
                    @click="handleCompleteTask(t)"
                  >
                    任务完成
                  </el-button>
                  <el-button size="small" text type="primary" @click="openTaskDetail(t.id)">查看详情</el-button>
                </div>
              </div>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<style scoped>
.tasks-page {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 20px 16px 32px;
}

.top-nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  column-gap: 24px;
  padding: 12px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
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

.nav-placeholder {
  width: 80px;
}

.search-section {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1 1 260px;
}

.search-actions {
  display: flex;
  gap: 8px;
}

.filter-button {
  border-color: #e5e7eb;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  color: #6b7280;
}

.filter-footer {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}

.tasks-section {
  margin-top: 22px;
}

.tasks-header {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tasks-header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tasks-title {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: #111827;
}

.tasks-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.tasks-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.06);
  color: #1d4ed8;
  font-size: 12px;
}

.tasks-badge-number {
  min-width: 20px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #2563eb;
  color: #f9fafb;
  font-weight: 600;
  text-align: center;
}

.tasks-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  padding: 10px 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.9), rgba(239, 246, 255, 0.95));
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.metric-card-muted {
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.9), rgba(243, 244, 246, 0.95));
}

.metric-label {
  font-size: 11px;
  color: #6b7280;
}

.metric-value {
  margin-top: 4px;
  font-size: 17px;
  font-weight: 650;
  color: #111827;
}

.metric-value-green {
  color: #16a34a;
}

.metric-value-blue {
  color: #2563eb;
}

.metric-hint {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
}

.task-card {
  border-radius: 18px;
  margin-bottom: 14px;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: radial-gradient(circle at top left, rgba(239, 246, 255, 0.9), #ffffff);
}

.task-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.task-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  font-size: 12px;
}

.task-tag-icon {
  font-size: 14px;
}

.task-title {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-meta {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.task-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 卡片上“我的任务”标记行 */
.task-mine-badge {
  margin-top: 2px;
}

.task-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reward-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.reward {
  display: flex;
  flex-direction: column;
}

.remaining-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.remaining-time .el-icon {
  font-size: 14px;
}

.reward-label {
  font-size: 11px;
  color: #6b7280;
}

.reward-value {
  font-size: 16px;
  font-weight: 650;
  color: #16a34a;
}

.task-actions {
  display: flex;
  gap: 8px;
}

/* 发布任务弹窗样式（对齐首页欢迎风格） */
.publish-task-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 14px 18px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.publish-task-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.publish-task-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.16), rgba(129, 140, 248, 0.16));
  color: #1d4ed8;
}

.publish-task-header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.publish-task-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.publish-task-sub {
  font-size: 12px;
  color: #6b7280;
}

.publish-task-body {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 16px;
  padding: 12px 18px 4px;
}

.publish-task-main {
  min-width: 0;
}

.publish-task-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.publish-task-section {
  padding: 10px 12px 12px;
  border-radius: 14px;
  background: #f9fafb;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.publish-task-section-grid {
  margin-top: 6px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
}

.section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
}

.publish-task-aside {
  align-self: stretch;
  padding: 10px 12px 12px;
  border-radius: 14px;
  background: #fefce8;
  border: 1px solid #facc15;
  font-size: 12px;
  color: #854d0e;
}

.aside-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.aside-list {
  margin: 0;
  padding-left: 16px;
  line-height: 1.6;
}

.aside-list li + li {
  margin-top: 2px;
}

.publish-task-footer {
  padding: 8px 18px 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .publish-task-body {
    grid-template-columns: 1fr;
  }

  .section-grid {
    grid-template-columns: 1fr;
  }
}

.detail-actions {
  margin-top: 16px;
}

.my-tasks-hint {
  font-size: 12px;
  color: #6b7280;
  margin: 0 0 12px 0;
}

.my-task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.my-task-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.my-task-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.my-task-meta {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.my-task-deadline {
  font-size: 12px;
  color: #1d4ed8;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.my-task-deadline-raw {
  color: #9ca3af;
  font-size: 11px;
}

.my-task-status {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}

.my-task-item-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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
}

@media (max-width: 640px) {
  .search-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-actions {
    justify-content: flex-end;
  }
}
</style>

