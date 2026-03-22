import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

/**
 * 全局错误处理：部分手机浏览器若报错会导致白屏，这里尝试显示友好提示（仅展示一次）。
 */
let loadErrorShown = false
function showLoadError() {
  if (loadErrorShown) return
  loadErrorShown = true
  const app = document.getElementById('app')
  if (!app || app.querySelector('#app-load-error')) return
  const loading = document.getElementById('app-loading')
  if (loading) loading.remove()
  const div = document.createElement('div')
  div.id = 'app-load-error'
  div.style.cssText = 'display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;background:#f1f5f9;color:#64748b;font-size:14px;font-family:system-ui,sans-serif;padding:20px;text-align:center;'
  div.innerHTML = '<p style="color:#dc2626;margin-bottom:8px;">页面加载失败</p><p>请尝试刷新、使用 Chrome / Safari 最新版，或检查网络后重试。</p>'
  app.appendChild(div)
}

window.addEventListener('error', function (e) {
  if (e.message && e.filename && e.message.indexOf('ResizeObserver') === -1) showLoadError()
})

/**
 * 创建 Vue 根应用，并挂载全局插件。
 */
const app = createApp(App)

app.use(ElementPlus)
app.use(router)

app.mount('#app')
