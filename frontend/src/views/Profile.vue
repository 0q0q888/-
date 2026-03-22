<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Camera,
  EditPen,
  Lock,
  School,
  UserFilled,
  Wallet,
} from '@element-plus/icons-vue'
import axios from 'axios'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const router = useRouter()

const currentUserId = ref(null)
const profile = ref({
  id: null,
  username: '',
  nickname: '',
  avatarUrl: '',
  gender: null,
  age: null,
  role: 'USER',
})
const walletBalance = ref('0.00')
const loading = ref(false)
const saving = ref(false)
const editForm = ref({
  nickname: '',
  avatarUrl: '',
  gender: null,
  age: null,
})

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

/** 编辑资料抽屉 */
const editDrawerVisible = ref(false)

/** 头像上传 */
const avatarInputRef = ref()
const avatarUploading = ref(false)

/** 头像圆形裁剪 */
const cropDialogVisible = ref(false)
const cropImgRef = ref()
const cropImgUrl = ref('')
const cropperInstance = ref(null)
const cropLoading = ref(false)
let cropObjectUrl = null

/** 修改密码弹窗 */
const pwdDialogVisible = ref(false)
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const pwdFormRef = ref()
const pwdLoading = ref(false)

/**
 * 个人中心顶部简易导航栏选中项。
 * 说明：
 * - 使用 Element Plus 菜单组件，在个人中心页也保留一条“全局导航”。
 */
const topNavActive = ref('profile')

/**
 * 顶部导航选择回调。
 *
 * @param {string} key 选中的菜单 key
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
    router.push('/messages')
    return
  }
  if (key === 'profile') {
    // 当前页，无需跳转
    return
  }
}

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

/** 头像占位图（Element Plus 风格） */
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const triggerAvatarPick = () => {
  avatarInputRef.value?.click?.()
}

const destroyCropper = () => {
  try {
    cropperInstance.value?.destroy?.()
  } catch (_) {}
  cropperInstance.value = null
  if (cropObjectUrl) {
    try {
      URL.revokeObjectURL(cropObjectUrl)
    } catch (_) {}
    cropObjectUrl = null
  }
  cropImgUrl.value = ''
}

const openCropDialog = async (file) => {
  if (!file) return
  if (!file.type?.startsWith?.('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  // 原图允许稍大一些，裁剪后再上传（最终仍受上传限制）
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('图片过大，请选择 8MB 以内的图片')
    return
  }
  destroyCropper()
  cropObjectUrl = URL.createObjectURL(file)
  cropImgUrl.value = cropObjectUrl
  cropDialogVisible.value = true
  await nextTick()
  const imgEl = cropImgRef.value
  if (!imgEl) return
  await new Promise((resolve) => {
    if (imgEl.complete && imgEl.naturalWidth) return resolve()
    imgEl.onload = () => resolve()
    imgEl.onerror = () => resolve()
  })
  cropperInstance.value = new Cropper(imgEl, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.85,
    responsive: true,
    background: false,
    guides: false,
    center: true,
    highlight: false,
    movable: true,
    zoomable: true,
    cropBoxMovable: true,
    cropBoxResizable: true,
    toggleDragModeOnDblclick: false,
  })
}

const uploadAvatar = async (file) => {
  if (!file) return
  if (!file.type?.startsWith?.('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 3 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 3MB')
    return
  }
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  avatarUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await axios.post('/api/upload/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const url = data?.url
    if (!url) {
      ElMessage.error('上传失败：未返回 url')
      return
    }
    editForm.value.avatarUrl = url
    profile.value.avatarUrl = url
    await axios.put(`/api/users/${currentUserId.value}`, { avatarUrl: url })
    ElMessage.success('头像已上传并保存')
  } catch (e) {
    ElMessage.error(e?.response?.data || '上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleAvatarFileChange = async (evt) => {
  const file = evt?.target?.files?.[0]
  // 清空 input，确保选择同一张也能触发 change
  if (evt?.target) evt.target.value = ''
  await openCropDialog(file)
}

const confirmCropAndUpload = async () => {
  if (!cropperInstance.value) return
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  cropLoading.value = true
  try {
    const canvas = cropperInstance.value.getCroppedCanvas({
      width: 512,
      height: 512,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high',
    })
    if (!canvas) {
      ElMessage.error('裁剪失败，请重试')
      return
    }
    const circleCanvas = document.createElement('canvas')
    circleCanvas.width = canvas.width
    circleCanvas.height = canvas.height
    const ctx = circleCanvas.getContext('2d')
    if (!ctx) {
      ElMessage.error('裁剪失败，请重试')
      return
    }
    ctx.clearRect(0, 0, circleCanvas.width, circleCanvas.height)
    ctx.save()
    ctx.beginPath()
    ctx.arc(circleCanvas.width / 2, circleCanvas.height / 2, circleCanvas.width / 2, 0, Math.PI * 2)
    ctx.closePath()
    ctx.clip()
    ctx.drawImage(canvas, 0, 0)
    ctx.restore()
    const blob = await new Promise((resolve) => circleCanvas.toBlob(resolve, 'image/png', 0.92))
    if (!blob) {
      ElMessage.error('裁剪失败，请重试')
      return
    }
    const croppedFile = new File([blob], `avatar_${Date.now()}.png`, { type: 'image/png' })
    await uploadAvatar(croppedFile)
    cropDialogVisible.value = false
  } finally {
    cropLoading.value = false
  }
}

const genderText = computed(() => {
  const g = profile.value?.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '未知'
})

const ageText = computed(() => {
  const a = profile.value?.age
  if (a == null || a === '') return '未设置'
  return `${a} 岁`
})

const loadProfile = async () => {
  if (!currentUserId.value) return
  loading.value = true
  try {
    const { data } = await axios.get(`/api/users/${currentUserId.value}`)
    profile.value = data
    editForm.value = {
      nickname: data.nickname || '',
      avatarUrl: data.avatarUrl || '',
      gender: data.gender ?? 0,
      age: data.age ?? null,
    }
  } catch (e) {
    ElMessage.error(e?.response?.data || '加载资料失败')
  } finally {
    loading.value = false
  }
}

const loadBalance = async () => {
  if (!currentUserId.value) return
  try {
    const { data } = await axios.get('/api/wallet/balance', {
      params: { userId: currentUserId.value },
    })
    const b = data?.balance ?? 0
    walletBalance.value = typeof b === 'number' ? b.toFixed(2) : String(b)
  } catch (e) {
    walletBalance.value = '0.00'
  }
}

const handleSaveProfile = async () => {
  if (!currentUserId.value) return
  saving.value = true
  try {
    await axios.put(`/api/users/${currentUserId.value}`, editForm.value)
    profile.value = { ...profile.value, ...editForm.value }
    ElMessage.success('保存成功')
    loadProfile()
    editDrawerVisible.value = false
  } catch (e) {
    ElMessage.error(e?.response?.data || '保存失败')
  } finally {
    saving.value = false
  }
}

const openPwdDialog = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdDialogVisible.value = true
}

const openEditDrawer = () => {
  editDrawerVisible.value = true
}

const handleBack = () => {
  router.push('/tasks')
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) callback(new Error('两次输入不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

const handleChangePassword = async () => {
  try {
    await pwdFormRef.value?.validate()
  } catch {
    return
  }
  pwdLoading.value = true
  try {
    await axios.post('/api/auth/change-password', pwdForm.value, {
      params: { userId: currentUserId.value },
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    localStorage.removeItem('currentUser')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e?.response?.data || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

const handleNavClick = (key) => {
  if (key === 'profile') return
  if (key === 'tasks') router.push('/tasks')
  else if (key === 'messages') router.push('/messages')
  else if (key === 'publish-task' || key === 'market') router.push('/home')
}

const handleUserCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('currentUser')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(() => {
  const raw = localStorage.getItem('currentUser')
  if (!raw) {
    router.push('/login')
    return
  }
  try {
    const u = JSON.parse(raw)
    currentUserId.value = u?.id ?? null
    if (!currentUserId.value) {
      router.push('/login')
      return
    }
    loadProfile()
    loadBalance()
    refreshUnreadTotal()
  } catch (e) {
    router.push('/login')
  }
})

onUnmounted(() => {
  destroyCropper()
})
</script>

<template>
  <div class="profile-shell">
    <!-- 顶部导航栏：与任务大厅保持同一风格 -->
    <header class="top-nav">
      <div class="brand">
        <el-icon class="brand-logo"><School /></el-icon>
        <div class="brand-text">
          <div class="brand-title">校园即时服务系统</div>
          <div class="brand-subtitle">个人中心 · Profile</div>
        </div>
      </div>

      <nav class="nav-center">
        <button type="button" class="nav-item" @click="handleTopNavSelect('tasks')">
          任务大厅
        </button>
        <button type="button" class="nav-item" @click="handleTopNavSelect('market')">
          交易市场
        </button>
        <button type="button" class="nav-item" @click="handleTopNavSelect('messages')">
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
        <button type="button" class="nav-item active" @click="handleTopNavSelect('profile')">
          个人中心
        </button>
      </nav>

      <div class="nav-right">
        <div class="nav-user">
          <div class="user-info">
            <div class="user-name">
              {{ profile.nickname || profile.username || '用户' }}
            </div>
          </div>
          <div class="user-avatar-wrapper">
            <img class="user-avatar" :src="profile.avatarUrl || defaultAvatar" alt="用户头像" />
          </div>
        </div>
      </div>
    </header>
    <input
      ref="avatarInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleAvatarFileChange"
    />

    <div class="hero">
      <div class="hero-bg" />

      <div class="hero-topbar">
        <button type="button" class="back-btn" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="hero-title">
          <div class="hero-title-main">个人中心</div>
          <div class="hero-title-sub">Profile</div>
        </div>
        <div class="hero-actions">
          <el-button class="ghost-btn" size="small" @click="openEditDrawer">
            <el-icon style="margin-right: 4px"><EditPen /></el-icon>
            编辑
          </el-button>
        </div>
      </div>

      <div class="hero-user">
        <div class="avatar-wrap" @click="triggerAvatarPick">
          <el-avatar
            :size="78"
            :src="profile.avatarUrl || defaultAvatar"
            class="hero-avatar"
          >
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="avatar-badge">
            <el-icon><Camera /></el-icon>
          </div>
          <div v-if="avatarUploading" class="avatar-uploading">上传中…</div>
        </div>

        <div class="hero-user-info">
          <div class="hero-name">
            {{ profile.nickname || profile.username || '用户' }}
          </div>
          <div class="hero-meta">
            <span class="meta-pill">账号：{{ profile.username || '—' }}</span>
            <span class="meta-dot">·</span>
            <span class="meta-pill">性别：{{ genderText }}</span>
            <span class="meta-dot">·</span>
            <span class="meta-pill">年龄：{{ ageText }}</span>
          </div>
        </div>
      </div>

      <div class="hero-cards">
        <div class="stat-card" @click="loadBalance">
          <div class="stat-icon stat-icon-wallet">
            <el-icon><Wallet /></el-icon>
          </div>
          <div class="stat-main">
            <div class="stat-label">钱包余额</div>
            <div class="stat-value">￥{{ walletBalance }}</div>
          </div>
          <div class="stat-hint">点我刷新</div>
        </div>

        <div class="stat-card" @click="openPwdDialog">
          <div class="stat-icon stat-icon-safe">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="stat-main">
            <div class="stat-label">账户安全</div>
            <div class="stat-value stat-value-small">修改密码</div>
          </div>
          <div class="stat-hint">建议定期更新</div>
        </div>
      </div>
    </div>

    <main class="content">
      <el-skeleton v-if="loading" :rows="6" animated />
      <template v-else>
        <div class="panel">
          <div class="panel-title">常用功能</div>
          <div class="cell-list">
            <button type="button" class="cell" @click="openEditDrawer">
              <div class="cell-left">
                <span class="cell-icon cell-icon-blue">
                  <el-icon><EditPen /></el-icon>
                </span>
                <span class="cell-text">编辑个人资料</span>
              </div>
              <div class="cell-right">完善昵称/性别/年龄</div>
            </button>

            <button type="button" class="cell" @click="loadBalance">
              <div class="cell-left">
                <span class="cell-icon cell-icon-green">
                  <el-icon><Wallet /></el-icon>
                </span>
                <span class="cell-text">钱包余额</span>
              </div>
              <div class="cell-right">￥{{ walletBalance }}</div>
            </button>

            <button type="button" class="cell" @click="openPwdDialog">
              <div class="cell-left">
                <span class="cell-icon cell-icon-orange">
                  <el-icon><Lock /></el-icon>
                </span>
                <span class="cell-text">修改密码</span>
              </div>
              <div class="cell-right">保护账号安全</div>
            </button>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">系统</div>
          <div class="cell-list">
            <button type="button" class="cell" @click="handleNavClick('tasks')">
              <div class="cell-left">
                <span class="cell-icon cell-icon-purple">
                  <el-icon><School /></el-icon>
                </span>
                <span class="cell-text">返回任务大厅</span>
              </div>
              <div class="cell-right">浏览任务</div>
            </button>

            <button type="button" class="cell cell-danger" @click="handleUserCommand('logout')">
              <div class="cell-left">
                <span class="cell-icon cell-icon-danger">
                  <el-icon><ArrowLeft /></el-icon>
                </span>
                <span class="cell-text">退出登录</span>
              </div>
              <div class="cell-right" />
            </button>
          </div>
        </div>
      </template>
    </main>

    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="cropDialogVisible"
      title="裁剪头像（圆形）"
      width="520px"
      :close-on-click-modal="false"
      @closed="destroyCropper"
    >
      <div class="cropper-shell">
        <div class="cropper-stage">
          <img
            v-if="cropImgUrl"
            ref="cropImgRef"
            :src="cropImgUrl"
            alt="裁剪头像"
            class="cropper-img"
          />
        </div>
        <div class="cropper-tip">可拖动图片、滚轮/双指缩放，调整裁剪框大小与位置</div>
      </div>
      <template #footer>
        <el-button @click="cropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cropLoading" @click="confirmCropAndUpload">确定并上传</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="editDrawerVisible" title="编辑个人资料" size="440px" direction="rtl">
      <div class="drawer-head">
        <div class="drawer-avatar">
          <el-avatar
            :size="72"
            :src="editForm.avatarUrl || defaultAvatar"
            style="cursor: pointer"
            @click="triggerAvatarPick"
          >
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="drawer-avatar-tips">点击头像选择本地图片</div>
        </div>
      </div>

      <el-form label-position="top" class="drawer-form">
        <el-form-item label="用户名">
          <el-input v-model="profile.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio v-for="opt in genderOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="editForm.age" :min="1" :max="120" placeholder="年龄" />
        </el-form-item>
        <div class="drawer-actions">
          <el-button @click="editDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<style scoped>
.profile-shell {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

/* 顶部导航样式：直接复用任务大厅的导航风格 */
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
  cursor: default;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s ease;
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

.hero {
  position: relative;
  margin: 14px 0 14px;
  border-radius: 22px;
  overflow: hidden;
  box-shadow:
    0 24px 70px rgba(15, 23, 42, 0.16),
    0 0 0 1px rgba(226, 232, 240, 0.55);
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 520px at 10% 0%, rgba(99, 102, 241, 0.52), transparent 60%),
    radial-gradient(900px 420px at 95% 20%, rgba(56, 189, 248, 0.42), transparent 55%),
    radial-gradient(820px 520px at 35% 105%, rgba(34, 197, 94, 0.16), transparent 55%),
    linear-gradient(135deg, rgba(17, 24, 39, 0.86), rgba(2, 6, 23, 0.92));
  transform: scale(1.02);
}

/* 响应式适配：窄屏下让顶部导航与布局更友好 */
@media (max-width: 900px) {
  .profile-shell {
    padding-inline: 12px;
  }
}

.hero-topbar {
  position: relative;
  z-index: 1;
  padding: 14px 14px 6px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
}

.back-btn {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
  display: grid;
  place-items: center;
  cursor: pointer;
  backdrop-filter: blur(14px);
  transition: transform 140ms ease, background 140ms ease;
}

.back-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.12);
}

.hero-title {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.hero-title-main {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
  letter-spacing: 0.04em;
}

.hero-title-sub {
  margin-top: 3px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
}

.ghost-btn {
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

.hero-user {
  position: relative;
  z-index: 1;
  padding: 10px 18px 14px;
  display: flex;
  gap: 14px;
  align-items: center;
}

.avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.hero-avatar {
  box-shadow:
    0 22px 46px rgba(0, 0, 0, 0.35),
    0 0 0 4px rgba(255, 255, 255, 0.12);
}

.avatar-badge {
  position: absolute;
  right: -6px;
  bottom: -6px;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
}

.avatar-uploading {
  position: absolute;
  left: 50%;
  bottom: -20px;
  transform: translateX(-50%);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.76);
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
}

.cropper-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cropper-stage {
  width: 100%;
  height: 320px;
  border-radius: 14px;
  overflow: hidden;
  background: #0b1220;
  border: 1px solid rgba(226, 232, 240, 0.24);
}

.cropper-img {
  display: block;
  max-width: 100%;
}

.cropper-tip {
  font-size: 12px;
  color: #64748b;
}

.hero-user-info {
  min-width: 0;
  flex: 1;
}

.hero-name {
  font-size: 20px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.96);
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.70);
  font-size: 12px;
}

.meta-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(14px);
}

.meta-dot {
  opacity: 0.5;
}

.hero-cards {
  position: relative;
  z-index: 1;
  padding: 0 14px 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  cursor: pointer;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
  padding: 12px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(18px);
  transition: transform 140ms ease, background 140ms ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.13);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.92);
}

.stat-icon-wallet {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.38), rgba(16, 185, 129, 0.20));
}

.stat-icon-safe {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.36), rgba(245, 158, 11, 0.18));
}

.stat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.stat-value {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.96);
}

.stat-value-small {
  font-size: 15px;
}

.stat-hint {
  grid-column: 2 / 3;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
}

.content {
  padding: 12px 2px 0;
}

.panel {
  margin-top: 12px;
  padding: 14px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.panel-title {
  font-size: 12px;
  color: #6b7280;
  letter-spacing: 0.08em;
  margin-bottom: 10px;
}

.cell-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cell {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  border-radius: 14px;
  padding: 12px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: transform 140ms ease, box-shadow 140ms ease;
}

.cell:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.cell-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.cell-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #0f172a;
  background: rgba(148, 163, 184, 0.16);
}

.cell-icon-blue {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.cell-icon-green {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.cell-icon-orange {
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
}

.cell-icon-purple {
  background: rgba(139, 92, 246, 0.14);
  color: #6d28d9;
}

.cell-icon-danger {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.cell-text {
  font-size: 14px;
  font-weight: 650;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-right {
  font-size: 12px;
  color: #6b7280;
}

.cell-danger {
  border-color: rgba(239, 68, 68, 0.18);
}

.drawer-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drawer-avatar {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.drawer-avatar-tips {
  font-size: 12px;
  color: #6b7280;
}

.drawer-form {
  margin-top: 14px;
}

.drawer-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .hero-cards {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* 圆形裁剪视觉：让 Cropper 的裁剪框呈圆形 */
.cropper-view-box,
.cropper-face {
  border-radius: 50%;
}
</style>
