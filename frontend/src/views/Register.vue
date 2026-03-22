<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

/**
 * Vue Router 实例。
 * 说明：
 * - 注册成功后跳转回登录页，让用户使用新账号登录。
 */
const router = useRouter()

/**
 * 注册表单的数据模型。
 * 字段说明：
 * - username：登录用户名（唯一）；
 * - nickname：展示用昵称；
 * - password：登录密码；
 * - confirmPassword：确认密码（前端校验两次输入是否一致）。
 */
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

/**
 * Element Plus 表单组件的引用。
 * 说明：
 * - 用于在点击“注册”按钮时触发表单校验。
 */
const formRef = ref()

/**
 * 表单校验规则。
 * 说明：
 * - 用户名必填；
 * - 昵称必填；
 * - 密码必填且长度不少于 6 位（可根据需要调整）；
 * - 确认密码必填，并且与密码保持一致。
 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
        } else if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

/**
 * 是否正在提交，用于控制注册按钮的 loading 状态。
 */
const loading = ref(false)

/**
 * 处理注册提交的方法。
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
      // 说明：此处仅注册“普通用户”，后端可默认 role = USER
      await axios.post('/api/auth/register', {
        username: form.username,
        nickname: form.nickname,
        password: form.password,
      })

      ElMessage.success('注册成功，请使用新账号登录')

      // 注册成功后跳转回登录页
      router.push('/login')
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        '注册失败，请稍后重试'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}

/**
 * 跳转到登录页的方法。
 * 说明：
 * - 当用户本来就有账号，可以通过底部链接快速返回登录页面。
 */
const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="auth-layout">
    <div class="auth-shell">
      <!-- 左侧：说明注册只创建普通用户 -->
      <aside class="auth-aside">
        <div class="system-title">
          <div class="system-name">校园即时服务系统</div>
          <div class="system-subtitle">Campus Instant Service Platform</div>
        </div>

        <div class="mascot-strip">
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

      <!-- 右侧：注册表单 -->
      <main class="auth-main">
        <div class="form-card">
          <div class="form-title">校园即时服务系统 · 注册</div>
          <div class="form-subtitle">仅创建普通用户账号，管理员由系统分配</div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="register-form"
            @submit="handleSubmit"
          >
            <el-form-item label="用户名（用于登录）" prop="username">
              <el-input
                v-model="form.username"
                size="large"
                placeholder="请输入登录用户名"
                autocomplete="username"
              />
            </el-form-item>

            <el-form-item label="昵称（展示名称）" prop="nickname">
              <el-input
                v-model="form.nickname"
                size="large"
                placeholder="请输入昵称"
                autocomplete="nickname"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                show-password
                placeholder="请输入密码"
                autocomplete="new-password"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                size="large"
                type="password"
                show-password
                placeholder="请再次输入密码"
                autocomplete="new-password"
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
                注册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="footer-text">
            已有账号？
            <a class="link" @click="goToLogin">返回登录</a>
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
</style>

