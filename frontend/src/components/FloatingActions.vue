<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  EditPen,
  ShoppingBag,
  Message,
  Service,
  Tickets,
  UserFilled,
} from '@element-plus/icons-vue'

const router = useRouter()

const STORAGE_KEY = 'floating-actions-edge'
const CARD_W = 52
const CARD_H = 320
const COLLAPSED_SIZE = 20
const COLLAPSED_LENGTH = 56

/** 贴边：'left' | 'right' */
const edge = ref('right')
/** 沿边的偏移（px） */
const offset = ref(0)
/** 是否收起（只显示一条边条） */
const collapsed = ref(false)
/** 正在拖动 */
const isDragging = ref(false)
/** 拖动时用于阻止按钮点击 */
const didDrag = ref(false)
/** 拖动中瞬时位置 */
const dragPos = ref({ left: null, top: null })
/** 有指针/触摸按下时置 true，松开后延迟置 false，避免缩放刚结束就更新视口导致卡顿 */
const interactionLock = ref(false)
let interactionLockTimer = null
/**
 * 视口信息（优先 visualViewport）：
 * - x/y: 可视区域左上角在 layout viewport 中的偏移（缩放/地址栏变化时会变化）
 * - w/h: 可视区域宽高
 *
 * 用它来确保悬浮窗在手机缩放后仍能贴到“可视区域”的左右边缘，不会飘到看不见的位置。
 */
const viewport = ref({
  x: 0,
  y: 0,
  w: typeof window !== 'undefined' ? window.innerWidth : 375,
  h: typeof window !== 'undefined' ? window.innerHeight : 667,
})

function getViewport() {
  const v = window.visualViewport
  if (v) {
    return {
      x: Math.round(v.offsetLeft),
      y: Math.round(v.offsetTop),
      w: Math.round(v.width),
      h: Math.round(v.height),
    }
  }
  return { x: 0, y: 0, w: window.innerWidth, h: window.innerHeight }
}

let viewportThrottleTimer = null
const VIEWPORT_THROTTLE_MS = 350
function updateViewportThrottled() {
  if (isDragging.value || interactionLock.value) return
  if (viewportThrottleTimer) return
  viewportThrottleTimer = setTimeout(() => {
    viewportThrottleTimer = null
    if (interactionLock.value) return
    viewport.value = getViewport()
    offset.value = clampOffset(offset.value, edge.value)
  }, VIEWPORT_THROTTLE_MS)
}

function loadSaved() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const o = JSON.parse(raw)
      if (['left', 'right', 'top', 'bottom'].includes(o.edge)) {
        edge.value = o.edge === 'top' || o.edge === 'bottom' ? 'right' : o.edge
        offset.value = clampOffset(o.offset, edge.value)
        collapsed.value = false
        return
      }
    }
  } catch (_) {}
  edge.value = 'right'
  const { h } = viewport.value
  offset.value = Math.max(0, Math.floor((h - CARD_H) / 2))
  collapsed.value = false
}

function clampOffset(val, e) {
  const v = Number(val)
  const { w, h } = viewport.value
  if (e === 'left' || e === 'right') {
    return Math.max(0, Math.min(v, h - CARD_H))
  }
  return Math.max(0, Math.min(v, w - CARD_W))
}

function save() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      edge: edge.value,
      offset: offset.value,
      collapsed: collapsed.value,
    }))
  } catch (_) {}
}

/** 根据 edge + offset 算出 left, top（仅左右贴边）；收起时用窄条尺寸 */
function edgeToPos() {
  const { x: X, y: Y, w: W, h: H } = viewport.value
  const o = offset.value
  const isColl = collapsed.value
  if (isColl) {
    if (edge.value === 'left') {
      return { left: X, top: Y + Math.max(0, Math.min(o, H - COLLAPSED_LENGTH)) }
    }
    return { left: X + W - COLLAPSED_SIZE, top: Y + Math.max(0, Math.min(o, H - COLLAPSED_LENGTH)) }
  }
  if (edge.value === 'left') {
    return { left: X, top: Y + clampOffset(o, 'left') }
  }
  return { left: X + W - CARD_W, top: Y + clampOffset(o, 'right') }
}

/** 只贴左右边：从中心 (cx, cy) 算最近一侧 */
function nearestEdge(cx, cy) {
  const { x: X, y: Y, w: W, h: H } = viewport.value
  const dl = cx - X
  const dr = X + W - cx
  if (dl <= dr) {
    return { edge: 'left', offset: Math.max(0, Math.min(cy - Y - CARD_H / 2, H - CARD_H)) }
  }
  return { edge: 'right', offset: Math.max(0, Math.min(cy - Y - CARD_H / 2, H - CARD_H)) }
}

const positionStyle = computed(() => {
  const inDrag = isDragging.value && dragPos.value.left != null && dragPos.value.top != null
  const left = inDrag ? dragPos.value.left : edgeToPos().left
  const top = inDrag ? dragPos.value.top : edgeToPos().top
  return {
    left: `${left}px`,
    top: `${top}px`,
    transition: isDragging.value ? 'none' : 'left 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), top 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)',
  }
})

const cardClass = computed(() => [
  'floating-card',
  `floating-card--${edge.value}`,
  { 'floating-card--collapsed': collapsed.value },
])

const cardStyle = computed(() => {
  const base = { transition: 'width 0.35s cubic-bezier(0.4, 0, 0.2, 1), height 0.35s cubic-bezier(0.4, 0, 0.2, 1)' }
  if (collapsed.value) {
    return { ...base, width: `${COLLAPSED_SIZE}px`, minHeight: `${COLLAPSED_LENGTH}px` }
  }
  return { ...base, width: `${CARD_W}px`, minHeight: `${CARD_H}px` }
})

let dragStart = null
const DRAG_THRESHOLD = 8
const INTERACTION_LOCK_MS = 500
let moveRaf = null

function setInteractionLock(on) {
  if (interactionLockTimer) {
    clearTimeout(interactionLockTimer)
    interactionLockTimer = null
  }
  interactionLock.value = on
  if (!on) return
  interactionLockTimer = setTimeout(() => {
    interactionLockTimer = null
    interactionLock.value = false
  }, INTERACTION_LOCK_MS)
}

function startDrag(clientX, clientY) {
  didDrag.value = false
  const pos = dragPos.value.left != null ? dragPos.value : edgeToPos()
  dragStart = {
    startX: clientX,
    startY: clientY,
    left: pos.left,
    top: pos.top,
  }
  dragPos.value = { left: pos.left, top: pos.top }
  isDragging.value = true
  setInteractionLock(true)
}

function applyMove(clientX, clientY) {
  if (!dragStart) return
  const dx = clientX - dragStart.startX
  const dy = clientY - dragStart.startY
  if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
    if (!didDrag.value) {
      didDrag.value = true
      collapsed.value = true
      const cx = dragStart.left + CARD_W / 2
      const cy = dragStart.top + CARD_H / 2
      dragStart.left = cx - COLLAPSED_SIZE / 2
      dragStart.top = cy - COLLAPSED_LENGTH / 2
      dragStart.startX = clientX
      dragStart.startY = clientY
      dragPos.value = { left: dragStart.left, top: dragStart.top }
    }
  }
  if (!didDrag.value) return
  const dx2 = clientX - dragStart.startX
  const dy2 = clientY - dragStart.startY
  const { x: X, y: Y, w: W, h: H } = viewport.value
  let left = dragStart.left + dx2
  let top = dragStart.top + dy2
  left = Math.max(X, Math.min(left, X + W - COLLAPSED_SIZE))
  top = Math.max(Y, Math.min(top, Y + H - COLLAPSED_LENGTH))
  dragPos.value = { left, top }
}

function moveDrag(clientX, clientY) {
  if (!dragStart) return
  if (moveRaf) return
  moveRaf = requestAnimationFrame(() => {
    moveRaf = null
    applyMove(clientX, clientY)
  })
}

function endDrag() {
  if (moveRaf) {
    cancelAnimationFrame(moveRaf)
    moveRaf = null
  }
  if (!dragStart) return
  if (!didDrag.value) {
    isDragging.value = false
    dragPos.value = { left: null, top: null }
    dragStart = null
    setInteractionLock(true)
    return
  }
  const cx = dragPos.value.left + COLLAPSED_SIZE / 2
  const cy = dragPos.value.top + COLLAPSED_LENGTH / 2
  const { edge: newEdge, offset: newOffset } = nearestEdge(cx, cy)
  edge.value = newEdge
  offset.value = newOffset
  collapsed.value = false
  isDragging.value = false
  dragPos.value = { left: null, top: null }
  dragStart = null
  setInteractionLock(true)
  save()
}

function onPointerDown(e) {
  if (e.button !== 0 && e.pointerType === 'mouse') return
  const x = e.clientX
  const y = e.clientY
  startDrag(x, y)
  document.addEventListener('pointermove', onPointerMove, true)
  document.addEventListener('pointerup', onPointerUp, true)
  document.addEventListener('pointercancel', onPointerUp, true)
}

function onPointerMove(e) {
  if (didDrag.value) e.preventDefault()
  moveDrag(e.clientX, e.clientY)
}

function onPointerUp() {
  document.removeEventListener('pointermove', onPointerMove, true)
  document.removeEventListener('pointerup', onPointerUp, true)
  document.removeEventListener('pointercancel', onPointerUp, true)
  endDrag()
}

function onCardClick(e) {
  if (didDrag.value) {
    e.preventDefault()
    e.stopPropagation()
  }
}

onMounted(() => {
  viewport.value = getViewport()
  loadSaved()
  window.addEventListener('resize', updateViewportThrottled)
  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', updateViewportThrottled)
  }
  refreshUnreadTotal()
  unreadTimer = setInterval(refreshUnreadTotal, 1000)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewportThrottled)
  if (window.visualViewport) {
    window.visualViewport.removeEventListener('resize', updateViewportThrottled)
  }
  if (viewportThrottleTimer) clearTimeout(viewportThrottleTimer)
  if (interactionLockTimer) clearTimeout(interactionLockTimer)
  if (moveRaf) cancelAnimationFrame(moveRaf)
  document.removeEventListener('pointermove', onPointerMove, true)
  document.removeEventListener('pointerup', onPointerUp, true)
  document.removeEventListener('pointercancel', onPointerUp, true)
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})

/**
 * 悬浮窗按钮点击统一跳转处理。
 *
 * @param {string} key 业务含义标识：tasks / publish-task / market / messages / ai-service / profile
 */
const handleClick = (key) => {
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
  if (key === 'ai-service') {
    router.push('/ai-customer-service')
    return
  }
  if (key === 'profile') {
    router.push('/profile')
    return
  }
}

/** 悬浮窗中“消息”按钮的小红点：未读总数（任务 + 商品） */
const unreadTotal = ref(0)
let unreadTimer = null

const refreshUnreadTotal = async () => {
  try {
    const raw = localStorage.getItem('currentUser')
    if (!raw) {
      unreadTotal.value = 0
      return
    }
    const u = JSON.parse(raw)
    const id = u?.id
    if (!id) {
      unreadTotal.value = 0
      return
    }
    const { data } = await axios.get('/api/messages/unread-total', {
      params: { userId: id },
    })
    unreadTotal.value = Number.isFinite(Number(data)) ? Number(data) : 0
  } catch {
    // 静默失败
  }
}
</script>

<template>
  <aside
    class="floating-actions"
    :style="positionStyle"
  >
    <div
      :class="cardClass"
      :style="cardStyle"
      @pointerdown="onPointerDown"
      @click.capture="onCardClick"
    >
      <!-- 拖动中收起：只显示一条窄条 -->
      <template v-if="collapsed">
        <span class="floating-drag-dots" aria-hidden="true">⋯</span>
      </template>
      <template v-else>
        <button
          type="button"
          class="floating-item"
          @click="handleClick('publish-task')"
        >
          <el-icon class="floating-icon floating-icon-blue"><EditPen /></el-icon>
          <span class="floating-label">发任务</span>
        </button>
        <button
          type="button"
          class="floating-item"
          @click="handleClick('tasks')"
        >
          <el-icon class="floating-icon floating-icon-green"><Tickets /></el-icon>
          <span class="floating-label">任务大厅</span>
        </button>
        <button
          type="button"
          class="floating-item"
          @click="handleClick('market')"
        >
          <el-icon class="floating-icon floating-icon-orange"><ShoppingBag /></el-icon>
          <span class="floating-label">交易市场</span>
        </button>
        <button type="button" class="floating-item" @click="handleClick('messages')">
          <el-icon class="floating-icon floating-icon-purple"><Message /></el-icon>
          <span class="floating-label">
            消息
            <span
              v-if="unreadTotal > 0"
              class="floating-badge"
            >
              {{ unreadTotal }}
            </span>
          </span>
        </button>
        <button type="button" class="floating-item" @click="handleClick('ai-service')">
          <el-icon class="floating-icon floating-icon-cyan"><Service /></el-icon>
          <span class="floating-label">AI客服</span>
        </button>
        <button
          type="button"
          class="floating-item"
          @click="handleClick('profile')"
        >
          <el-icon class="floating-icon floating-icon-teal"><UserFilled /></el-icon>
          <span class="floating-label">个人中心</span>
        </button>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.floating-actions {
  position: fixed;
  z-index: 20;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
  -webkit-touch-callout: none;
}

.floating-actions:active {
  cursor: grabbing;
}

.floating-card {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  color: #0f172a;
  box-shadow:
    0 14px 30px rgba(15, 23, 42, 0.18),
    0 0 0 1px rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px;
  overflow: hidden;
}

.floating-card--collapsed {
  padding: 8px 6px;
}

.floating-drag-dots {
  color: #94a3b8;
  font-size: 14px;
  letter-spacing: 2px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 0;
}

.floating-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 2px;
  padding: 6px 0;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: inherit;
  font-size: 12px;
  cursor: pointer;
  transition:
    background-color 140ms ease,
    transform 140ms ease,
    box-shadow 140ms ease;
}

.floating-item:hover {
  background: rgba(248, 250, 252, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 8px 14px rgba(15, 23, 42, 0.18);
}

.floating-icon {
  font-size: 17px;
}

.floating-label {
  font-size: 10px;
  color: #4b5563;
}

.floating-badge {
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
  margin-left: 2px;
}

.floating-icon-blue {
  color: #2563eb;
}

.floating-icon-green {
  color: #16a34a;
}

.floating-icon-orange {
  color: #f97316;
}

.floating-icon-purple {
  color: #8b5cf6;
}

.floating-icon-cyan {
  color: #0891b2;
}

.floating-icon-teal {
  color: #0d9488;
}

</style>
