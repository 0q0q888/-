<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

/**
 * Vue Router 实例。
 * 说明：
 * - 用于在登录成功后跳转到主界面（/home）。
 */
const router = useRouter()

/**
 * 登录表单的数据模型。
 * 字段说明：
 * - username：登录用户名；
 * - password：登录密码。
 */
const form = reactive({
  username: '',
  password: '',
})

/**
 * Element Plus 表单组件的引用。
 * 说明：
 * - 通过该引用可以在提交时触发表单校验。
 */
const formRef = ref()

/**
 * 表单校验规则。
 * 说明：
 * - 用户名必填；
 * - 密码必填。
 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/**
 * 是否正在提交，用于控制登录按钮的 loading 状态。
 */
const loading = ref(false)

/**
 * 小猫小狗的情绪状态。
 * 说明：
 * - idle：默认状态；
 * - sad：登录失败时显示低落表情；
 * - happy：登录成功时短暂显示开心表情。
 */
const mascotMood = ref('idle')

/**
 * 登录卡片/窗口是否处于“震动”状态。
 * 说明：
 * - 登录失败时置为 true，触发 CSS 动画，短暂晃动后恢复。
 */
const isShaking = ref(false)

/**
 * 情绪状态恢复定时器句柄。
 * 说明：
 * - 每次触发“sad/happy”都重置定时器，避免多次点击导致状态错乱。
 */
const moodTimer = ref(0)

/**
 * 设置小猫小狗情绪，并在指定时间后自动恢复到 idle。
 *
 * @param {string} mood 情绪值（idle/happy/sad）
 * @param {number} durationMs 维持时长（毫秒）
 */
const setMoodTemporarily = (mood, durationMs) => {
  mascotMood.value = mood
  if (moodTimer.value) {
    window.clearTimeout(moodTimer.value)
  }
  moodTimer.value = window.setTimeout(() => {
    mascotMood.value = 'idle'
    moodTimer.value = 0
  }, durationMs)
}

/**
 * 处理登录提交的方法。
 *
 * @param {Event} event 原始提交事件，用于阻止浏览器默认提交行为
 */
const handleSubmit = (event) => {
  if (event) {
    event.preventDefault()
  }

  // 通过表单引用触发 Element Plus 的表单校验
  formRef.value?.validate(async (valid) => {
    if (!valid) {
      return
    }
    loading.value = true
    try {
      // 向后端登录接口发送 POST 请求
      const response = await axios.post('/api/auth/login', {
        username: form.username,
        password: form.password,
      })

      // 登录成功提示
      ElMessage.success(`登录成功，欢迎你：${response.data.nickname || response.data.username}`)
      setMoodTemporarily('happy', 900)

      // 保存当前用户基础信息到本地存储，供主页等页面使用
      localStorage.setItem('currentUser', JSON.stringify(response.data))

      // 登录成功后跳转到欢迎首页（/home）
      router.push('/home')
    } catch (error) {
      // 如果后端返回 4xx/5xx，统一提示为登录失败
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        '登录失败，请检查用户名或密码'
      ElMessage.error(msg)
      setMoodTemporarily('sad', 1600)
      // 登录失败时，让窗口轻微震动一下
      isShaking.value = true
      window.setTimeout(() => {
        isShaking.value = false
      }, 450)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="auth-layout">
    <div class="auth-shell" :class="{ 'is-shaking': isShaking }">
      <!-- 左侧：系统名称 + 可爱小猫小狗（简化版） -->
      <aside class="auth-aside">
        <div class="system-title">
          <div class="system-name">校园即时服务系统</div>
          <div class="system-subtitle">Campus Instant Service Platform</div>
        </div>

        <div class="mascot-strip" :class="mascotMood">
          <div class="pet-face cat">
            <div class="ear ear-left" />
            <div class="ear ear-right" />
            <div class="eyes">
              <span class="eye" />
              <span class="eye" />
            </div>
            <div class="mouth" />
          </div>
          <div class="pet-face dog">
            <div class="ear ear-left" />
            <div class="ear ear-right" />
            <div class="eyes">
              <span class="eye" />
              <span class="eye" />
            </div>
            <div class="mouth" />
          </div>
        </div>
      </aside>

      <!-- 右侧：表单区域 -->
      <main class="auth-main">
        <div class="form-card">
          <div class="form-title">校园即时服务系统 · 登录</div>
          <div class="form-subtitle">请输入账号密码进入系统</div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="login-form"
            @submit="handleSubmit"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                size="large"
                placeholder="请输入用户名"
                autocomplete="username"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                show-password
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="primary-button"
                :loading="loading"
                @click="handleSubmit"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="footer-text">
            还没有账号？
            <router-link class="link" to="/register">立即注册</router-link>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
}

.auth-shell {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.7);
  box-shadow:
    0 22px 60px rgba(15, 23, 42, 0.10),
    0 0 0 1px rgba(148, 163, 184, 0.20);
  backdrop-filter: blur(18px);
}

.auth-aside {
  padding: 34px 34px 30px;
  background:
    radial-gradient(circle at 0% 0%, rgba(191, 219, 254, 0.65) 0, transparent 60%),
    radial-gradient(circle at 100% 0%, rgba(221, 214, 254, 0.55) 0, transparent 60%),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.75), rgba(248, 250, 252, 0.75));
}

.system-title {
  margin-bottom: 24px;
}

.system-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #0f172a;
}

.system-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.mascot-strip {
  margin-top: 12px;
  display: flex;
  gap: 16px;
}

.pet-face {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: #f9fafb;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.10);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.pet-face.cat {
  border-radius: 26px;
}

.ear {
  position: absolute;
  top: 4px;
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background: #e0f2fe;
}

.cat .ear-left {
  left: 8px;
  transform: rotate(-12deg);
}

.cat .ear-right {
  right: 8px;
  transform: rotate(12deg);
}

.dog .ear-left {
  left: 2px;
  border-radius: 14px;
  height: 26px;
  background: #fee2e2;
}

.dog .ear-right {
  right: 2px;
  border-radius: 14px;
  height: 26px;
  background: #fee2e2;
}

.eyes {
  display: flex;
  gap: 14px;
  margin-top: 14px;
}

.eye {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #111827;
}

.mouth {
  width: 24px;
  height: 14px;
  margin-top: 8px;
  border-bottom: 2px solid #111827;
  border-radius: 0 0 18px 18px;
}

.mascot-strip.sad .mouth {
  border-radius: 18px 18px 0 0;
  border-top: 2px solid #111827;
  border-bottom: 0;
}

.mascot-strip.happy .mouth {
  height: 18px;
  border-radius: 0 0 999px 999px;
}

.auth-main {
  padding: 34px 34px 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.85);
}

.form-card {
  width: 100%;
  max-width: 360px;
  padding: 22px 22px 18px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.9);
  transition: transform 220ms ease, box-shadow 220ms ease;
}

.form-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.10);
}

.form-title {
  font-size: 18px;
  font-weight: 650;
  color: #0f172a;
}

.form-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

.primary-button {
  width: 100%;
  font-weight: 650;
  letter-spacing: 0.08em;
  border-radius: 999px;
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.primary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.20);
}

.primary-button:active {
  transform: translateY(0);
  box-shadow: none;
}

.footer-text {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

.link {
  color: #2563eb;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
  .auth-aside {
    display: none;
  }
  .auth-main {
    padding: 28px 18px;
  }
}

/* 登录失败时，窗口轻微晃动一下 */
@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-6px);
  }
  40% {
    transform: translateX(6px);
  }
  60% {
    transform: translateX(-4px);
  }
  80% {
    transform: translateX(4px);
  }
}

.is-shaking {
  animation: shake 0.4s ease;
}
</style>

