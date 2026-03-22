<script setup>
/**
 * 二手交易市场页面（类似闲鱼）
 * - 顶部搜索框按「标题」关键字筛选商品（前端过滤 + 可选后端 keyword 参数）
 * - 商品卡片显示首图 + 标题 + 价格 + 卖家昵称
 * - 卡片底部有「我想要」和「私聊我」两个按钮
 *   - 私聊我：直接调用消息接口创建 GOODS 类型会话，然后跳转到「我的消息」页面
 *   - 我想要：打开商品详情弹窗，点击「立即购买」创建订单
 * - 顶部右侧有「我的商品」抽屉（我买到的 / 我卖出的）
 * - 顶部右侧有「卖出宝贝」弹窗（标题 + 图片必填）；
 * - 卖出宝贝弹窗内新增「一键发布」功能：在已有图片的前提下，调用 YOLO 模型识别物品并自动填充标题，再复用原有发布逻辑。
 */

import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Picture,
  ShoppingCart,
  ChatDotRound,
  StarFilled,
  School,
} from '@element-plus/icons-vue'
import axios from 'axios'
import { suggestGoodsTitleByImage } from '../api/yolo'

const router = useRouter()

/**
 * 当前登录用户信息（与其它页面读取逻辑保持一致：从 localStorage 的 currentUser 中还原）
 */
const currentUserId = ref(null)
const currentUser = ref({
  id: null,
  nickname: '',
  avatarUrl: '',
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
 * 交易市场顶部全局导航栏当前激活菜单。
 * 说明：
 * - 用于 Element Plus 的 el-menu，使交易市场页也保留一条统一的顶栏导航。
 */
const topNavActive = ref('market')

/**
 * 顶部导航菜单点击处理。
 *
 * @param {string} key 菜单 key：tasks / market / messages / profile
 */
const handleTopNavSelect = (key) => {
  if (key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (key === 'market') {
    // 当前页，无需跳转
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
 * 顶部搜索关键字
 * 说明：
 * - 前端过滤时只匹配标题 title，符合你“根据 title 筛选”的需求；
 * - 同时把 keyword 传给后端，后端也可以根据标题做模糊查询和分页。
 */
const keyword = ref('')

/**
 * 商品列表数据 & 加载状态
 */
const goodsList = ref([])
const loadingGoods = ref(false)

/**
 * 当前选中的商品详情（右侧弹窗）
 */
const activeGoods = ref(null)
const detailVisible = ref(false)
const buying = ref(false)

/**
 * 发布商品（卖出宝贝）弹窗与表单数据
 */
const publishVisible = ref(false)
const publishForm = ref({
  title: '',
  description: '',
  price: null,
  images: '', // 用逗号分隔的图片地址，简单实现
})
const publishing = ref(false)

/**
 * 卖出宝贝：本地图片上传相关状态
 * 说明：
 * - imageUrls：已经成功上传到服务器的图片完整 URL 列表；
 * - uploadImageInputRef：隐藏的 <input type="file">，用于选择本地图片；
 * - imageUploading：是否正在上传图片，用于给出“上传中...”提示；
 * - 最终会把 imageUrls 用逗号拼接成字符串，写回 publishForm.images，保持与后端字段兼容。
 */
const imageUrls = ref([])
const uploadImageInputRef = ref(null)
const imageUploading = ref(false)

/** 一键发布的加载状态（避免与普通发布的 loading 混淆） */
const oneClickPublishing = ref(false)
/** 标记：是否在“一键发布”流程中等待用户先选择图片 */
const oneClickPending = ref(false)

/**
 * “我的商品” 抽屉：采用与“我的任务”相同的 Tab 形式（我买到的 / 我卖出的）
 */
const myGoodsVisible = ref(false)
const myGoodsTab = ref('bought') // bought | sold
const myBought = ref([])
const mySold = ref([])
const loadingMyGoods = ref(false)

/**
 * 默认占位图（当某个商品没有图片时使用）
 */
const defaultImg = 'https://via.placeholder.com/240x180?text=No+Image'

/** 默认用户头像（未设置时使用） */
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

/**
 * 判断某个商品是否是当前登录用户发布的。
 *
 * @param {object} g 商品对象
 * @returns {boolean} true 表示这是“我的商品”
 */
const isMyGoods = (g) => {
  if (!g || !currentUserId.value) return false
  return String(g.sellerId) === String(currentUserId.value)
}

/**
 * 根据后端返回的 images 字段（逗号分隔）取第一张图片；
 * 若不存在则回退到占位图
 */
const firstImage = (g) => {
  if (!g || !g.images) return defaultImg
  const arr = g.images
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return arr[0] || defaultImg
}

/**
 * 价格格式化：保留两位小数；null/空表示暂无报价；非法数值显示 "—"
 */
const formatPrice = (p) => {
  if (p == null || p === '') return '暂无报价'
  const n = Number(p)
  if (Number.isNaN(n)) return '—'
  return n.toFixed(2)
}

/**
 * 从后端加载商品列表。
 * 说明：
 * - 会把 keyword 一并传给后端，方便后续在后端做分页/模糊搜索；
 * - 同时前端也会用 keyword 做一次过滤，保证体验连贯。
 */
const loadGoods = async () => {
  loadingGoods.value = true
  try {
    const { data } = await axios.get('/api/goods', {
      params: { keyword: keyword.value.trim() || undefined },
    })
    goodsList.value = Array.isArray(data) ? data : []
  } catch (e) {
    const msg = e?.response?.data || e?.message || '加载商品失败'
    ElMessage.error(msg)
  } finally {
    loadingGoods.value = false
  }
}

/**
 * 打开某个商品的详情弹窗。
 * 如果你有独立的 /api/goods/{id} 接口，也可以在这里再请求一次以获取更完整信息。
 */
const openDetail = async (g) => {
  activeGoods.value = g
  detailVisible.value = true
}

/**
 * “我的商品”抽屉中点击订单查看详情。
 * 说明：
 * - 通过订单中的 goodsId 调用 GET /api/goods/{id} 拉取最新商品信息；
 * - 复用主界面的商品详情弹窗进行展示。
 *
 * @param {object} order 订单对象（myBought / mySold 列表中的一项）
 */
const openOrderGoodsDetail = async (order) => {
  if (!order || !order.goodsId) {
    ElMessage.warning('无法获取商品信息')
    return
  }
  try {
    const { data } = await axios.get(`/api/goods/${order.goodsId}`)
    activeGoods.value = data
    detailVisible.value = true
  } catch (e) {
    const msg = e?.response?.data || e?.message || '加载商品详情失败'
    ElMessage.error(msg)
  }
}

/**
 * 点击「我想要」：本质上就是打开详情弹窗，用户可在详情里点击「立即购买」。
 */
const handleWant = (g) => {
  openDetail(g)
}

/**
 * 点击「私聊我」：为当前商品和买家创建/复用一个订单，并发送首条聊天消息。
 * 说明：
 * - 调用后端 /api/goods/chat 接口，由后端负责创建订单 + 写入第一条 GOODS 类型消息；
 * - 前端在成功后只需要提示成功并跳转到“我的消息”页面。
 */
const handleChat = async (g) => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (!g.sellerId) {
    ElMessage.warning('商品卖家信息缺失')
    return
  }
  try {
    await axios.post('/api/goods/chat', null, {
      params: {
        goodsId: g.id,
        buyerId: currentUserId.value,
      },
    })
    ElMessage.success('已发送消息，正在打开商品交易消息')
    // 跳转到“我的消息”页面，并默认切换到“商品交易”tab
    router.push({ path: '/messages', query: { type: 'GOODS' } })
  } catch (e) {
    const msg = e?.response?.data || e?.message || '发送消息失败'
    ElMessage.error(msg)
  }
}

/**
 * 商品详情中点击「立即购买」：
 * - 调用 /api/goods/orders 创建订单；
 * - 下单成功后，可以可选发一条消息通知卖家；
 * - 资金流转和确认收货逻辑在后端实现，这里只负责触发创建订单。
 */
const handleBuyNow = async () => {
  const g = activeGoods.value
  if (!g) return
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (g.sellerId && g.sellerId === currentUserId.value) {
    ElMessage.warning('不能购买自己发布的商品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认以 ￥${formatPrice(g.price)} 购买「${g.title}」吗？`,
      '确认购买',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      },
    )
  } catch {
    // 用户点击取消
    return
  }

  buying.value = true
  try {
    const { data } = await axios.post('/api/goods/orders', {
      goodsId: g.id,
      buyerId: currentUserId.value,
    })
    ElMessage.success('下单成功，可在“我的商品”中查看订单')

    // 可选：下单成功后自动发一条消息给卖家
    try {
      await axios.post('/api/messages/send', {
        type: 'GOODS',
        bizId: data.id, // 此处以订单 ID 作为会话业务 ID
        senderId: currentUserId.value,
        receiverId: g.sellerId,
        content: `我已下单购买「${g.title}」，请尽快发货～`,
      })
    } catch (_) {
      // 聊天失败不影响下单本身
    }

    detailVisible.value = false
  } catch (e) {
    const msg = e?.response?.data || e?.message || '下单失败'
    ElMessage.error(msg)
  } finally {
    buying.value = false
  }
}

/**
 * 打开“卖出宝贝”弹窗。
 * 说明：title 与 images 为必填项，price 选填（不填则显示暂无报价，填则须非负）。
 */
const openPublish = () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  publishForm.value = {
    title: '',
    description: '',
    price: null,
    images: '',
  }
  imageUrls.value = []
  publishVisible.value = true
}

/**
 * 触发本地图片选择（点击“选择本地图片”按钮时调用）。
 */
const triggerImagePick = () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  uploadImageInputRef.value?.click()
}

/**
 * 处理本地图片选择并上传到后端：
 * - 使用 /api/upload/goods 接口，后端返回 { url: '图片访问地址' }；
 * - 支持一次选择多张图片，逐张上传，成功后加入 imageUrls；
 * - 同步更新 publishForm.images（逗号分隔），便于后端直接存入 t_goods.images。
 */
const handleImageFileChange = async (evt) => {
  const files = Array.from(evt?.target?.files || [])
  if (evt?.target) {
    // 清空 input，确保选择同一批文件也能再次触发 change
    evt.target.value = ''
  }
  if (!files.length) return
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }

  imageUploading.value = true
  try {
    for (const file of files) {
      if (!file.type || !file.type.startsWith('image/')) {
        ElMessage.warning('仅支持上传图片文件')
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      const { data } = await axios.post('/api/upload/goods', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const url = data?.url
      if (url) {
        imageUrls.value.push(url)
      }
    }
    publishForm.value.images = imageUrls.value.join(',')
    // 如果是“一键发布”流程触发的图片选择，上传完成后立即执行一键发布核心逻辑
    if (oneClickPending.value) {
      oneClickPending.value = false
      await runOneClickPublishCore()
    }
  } catch (e) {
    const msg = e?.response?.data || e?.message || '上传图片失败'
    ElMessage.error(msg)
  } finally {
    imageUploading.value = false
  }
}

/**
 * 从当前已上传图片列表中移除一张。
 * 移除后同步更新 publishForm.images 中逗号分隔的字符串。
 */
const removeImage = (index) => {
  if (index < 0 || index >= imageUrls.value.length) return
  imageUrls.value.splice(index, 1)
  publishForm.value.images = imageUrls.value.join(',')
}

/**
 * 提交发布商品：调用 POST /api/goods?sellerId=xxx
 */
const handlePublish = async () => {
  const { title, images, price, description } = publishForm.value
  if (!title || !title.trim()) {
    ElMessage.warning('请填写商品标题')
    return
  }
  if (!images || !images.trim()) {
    ElMessage.warning('请至少上传一张商品图片')
    return
  }
  if (price != null && price !== '' && Number(price) < 0) {
    ElMessage.warning('价格不能为负数')
    return
  }

  publishing.value = true
  try {
    const priceVal = (price != null && price !== '') ? Number(price) : null
    await axios.post(
      '/api/goods',
      {
        title: title.trim(),
        description: (description || '').trim(),
        price: priceVal,
        images: images.trim(),
      },
      { params: { sellerId: currentUserId.value } },
    )
    ElMessage.success('发布成功')
    publishVisible.value = false
    await loadGoods()
  } catch (e) {
    const msg = e?.response?.data || e?.message || '发布失败'
    ElMessage.error(msg)
  } finally {
    publishing.value = false
  }
}

/**
 * 一键发布核心逻辑：
 * - 要求已至少有一张已上传图片；
 * - 选择第一张图片作为主图，调用后端 YOLO 模块生成 title 建议（可选 desc），
 *   自动填充表单并复用 handlePublish() 完成发布。
 */
const runOneClickPublishCore = async () => {
  if (!imageUrls.value.length) {
    ElMessage.warning('请先上传至少一张商品图片')
    return
  }
  const mainImage = imageUrls.value[0]
  oneClickPublishing.value = true
  try {
    const { titleSuggest, descTemplate } = await suggestGoodsTitleByImage(mainImage)
    // 无论是否识别成功，都尽量填入一个标题，避免用户看起来“什么都没发生”
    if (titleSuggest && titleSuggest.trim()) {
      publishForm.value.title = titleSuggest.trim()
    } else if (!publishForm.value.title) {
      publishForm.value.title = '请填写标题（暂未识别到明确物品）'
      ElMessage.warning('暂未识别出合适的标题，请根据图片手动调整。')
    }
    // 仅在用户尚未填写描述时，使用模型给出的描述模板
    if (!publishForm.value.description && descTemplate) {
      publishForm.value.description = descTemplate
    }
    // 复用原有发布逻辑：进行字段校验并真正创建商品
    await handlePublish()
  } catch (e) {
    const msg = e?.response?.data || e?.message || '一键发布失败'
    ElMessage.error(msg)
  } finally {
    oneClickPublishing.value = false
  }
}

/**
 * 一键发布：
 * - 依赖已登录；
 * - 弹出确认对话框，提示“填入图片后立即发布”；
 * - 若当前尚未选择图片：记录 pending 状态并自动弹出文件选择对话框，图片上传完成后立即执行核心逻辑；
 * - 若已选择图片：直接执行核心逻辑（YOLO 识别 + 自动填表 + 发布）。
 */
const handleOneClickPublish = async () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      '系统将根据你选择的商品图片自动生成标题，并在图片就绪后立即发布该宝贝。确定继续吗？',
      '一键发布确认',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      },
    )
  } catch {
    // 用户点击取消
    return
  }

  if (!imageUrls.value.length) {
    // 尚未选择图片：标记 pending 并弹出文件选择，上传完成后由 handleImageFileChange 触发核心逻辑
    oneClickPending.value = true
    triggerImagePick()
    return
  }

  await runOneClickPublishCore()
}

/** 订单状态中文映射 */
const orderStatusText = (s) => {
  const m = { CREATED: '待支付', PAID: '待确认收货', FINISHED: '已完成', CANCELLED: '已取消' }
  return m[s] || s
}

const confirmingReceiptId = ref(null)

/**
 * 卖家确认收货：钱款转入卖家钱包。
 */
const handleConfirmReceipt = async (order) => {
  if (!order?.id || !currentUserId.value) return
  confirmingReceiptId.value = order.id
  try {
    await axios.post('/api/goods/orders/confirm-receipt', null, {
      params: { orderId: order.id, sellerId: currentUserId.value },
    })
    ElMessage.success('确认收货成功，钱款已到账')
    order.status = 'FINISHED'
  } catch (e) {
    const msg = e?.response?.data || e?.message || '操作失败'
    ElMessage.error(msg)
  } finally {
    confirmingReceiptId.value = null
  }
}

/**
 * 打开“我的商品”抽屉：
 * - /api/goods/my-bought?userId=xx => 我买到的
 * - /api/goods/my-sold?userId=xx  => 我卖出的
 */
const openMyGoods = async () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录')
    return
  }
  myGoodsTab.value = 'bought'
  myGoodsVisible.value = true
  loadingMyGoods.value = true
  try {
    const [boughtRes, soldRes] = await Promise.all([
      axios.get('/api/goods/my-bought', { params: { userId: currentUserId.value } }),
      axios.get('/api/goods/my-sold', { params: { userId: currentUserId.value } }),
    ])
    myBought.value = Array.isArray(boughtRes.data) ? boughtRes.data : []
    mySold.value = Array.isArray(soldRes.data) ? soldRes.data : []
  } catch (e) {
    const msg = e?.response?.data || e?.message || '加载我的商品失败'
    ElMessage.error(msg)
  } finally {
    loadingMyGoods.value = false
  }
}

/**
 * 组件挂载时：
 * - 从 localStorage 读取当前用户；
 * - 加载商品列表。
 */
onMounted(() => {
  const raw = window.localStorage.getItem('currentUser')
  if (raw) {
    try {
      const u = JSON.parse(raw)
      currentUserId.value = u?.id ?? null
      currentUser.value = {
        id: u?.id ?? null,
        nickname: u?.nickname || u?.username || '用户',
        avatarUrl: u?.avatarUrl || '',
      }
    } catch {
      // 解析失败则视为未登录
    }
  }
  refreshUnreadTotal()
  unreadTimer = window.setInterval(refreshUnreadTotal, 1000)
  loadGoods()
})

onUnmounted(() => {
  if (unreadTimer) {
    window.clearInterval(unreadTimer)
    unreadTimer = null
  }
})

/**
 * 前端过滤后的商品列表。
 * 说明：
 * - 只用标题 title 和关键字做包含判断（不再匹配 description），完全符合“根据 title 筛选商品”的要求；
 * - 后端接口也接收 keyword，便于你未来在数据库层面做更复杂的筛选或分页。
 */
const filteredGoods = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return goodsList.value
  return goodsList.value.filter((g) => {
    const t = `${g.title || ''}`.toLowerCase()
    return t.includes(kw)
  })
})
</script>

<template>
  <div class="goods-page">
    <!-- 顶部全局导航栏（与首页 / 个人中心风格保持一致） -->
    <header class="goods-nav-bar">
      <div class="goods-nav-inner">
        <div class="goods-nav-brand">
          <el-icon class="goods-nav-logo"><School /></el-icon>
          <span class="goods-nav-title">校园即时服务系统</span>
        </div>
        <el-menu
          class="goods-nav-menu"
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
        <div class="goods-nav-avatar-wrap">
          <div class="goods-nav-user" @click="router.push('/profile')">
            <span class="goods-nav-name">{{ currentUser.nickname }}</span>
            <img :src="currentUser.avatarUrl || defaultAvatar" class="goods-nav-avatar" alt="头像" />
          </div>
        </div>
      </div>
    </header>

    <!-- 本页搜索 + “我的商品 / 卖出宝贝”操作条 -->
    <header class="goods-topbar">
      <div class="goods-topbar-inner">
        <div class="goods-nav-left">
          <h1 class="title">交易市场</h1>
          <p class="subtitle">校园二手 · 灵感来自闲鱼</p>
        </div>
        <div class="goods-nav-center">
          <el-input
            v-model="keyword"
            class="search-input"
            size="large"
            placeholder="搜索你感兴趣的宝贝，如：耳机 / 图书 / 台灯"
            clearable
            @keyup.enter="loadGoods"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="goods-nav-right">
          <el-button size="large" @click="openMyGoods">
            <el-icon style="margin-right: 4px"><StarFilled /></el-icon>
            我的商品
          </el-button>
          <el-button type="primary" size="large" @click="openPublish">
            <el-icon style="margin-right: 4px"><Picture /></el-icon>
            卖出宝贝
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主区域：商品宫格 -->
    <main class="goods-main">
      <div class="goods-main-inner">
        <div v-if="loadingGoods" class="goods-loading">商品加载中…</div>
        <div v-else-if="filteredGoods.length === 0" class="goods-empty">
          <p>暂时没有符合条件的宝贝</p>
          <p class="desc">可以试试换个关键词，或者成为第一位发布者～</p>
        </div>
        <div v-else class="goods-grid">
          <div
            v-for="g in filteredGoods"
            :key="g.id"
            class="goods-card"
          >
            <!-- 商品首图区域：点击可打开详情 -->
            <div class="image-wrap" @click="openDetail(g)">
              <img :src="firstImage(g)" :alt="g.title" />
            </div>
            <div class="info">
              <div class="title" @click="openDetail(g)">
                {{ g.title }}
              </div>
              <!-- 如果是当前用户发布的商品，在卡片上标出“我的商品” -->
              <div v-if="isMyGoods(g)" class="mine-badge">
                <el-tag size="small" type="warning" effect="plain">我的商品</el-tag>
              </div>
              <div class="price-row">
                <span class="price">￥{{ formatPrice(g.price) }}</span>
                <span class="seller">卖家：{{ g.sellerNickname || '匿名' }}</span>
              </div>
              <div class="actions">
                <!-- 非本人商品：有报价显示“我想要 / 私聊我”，暂无报价仅显示“私聊我” -->
                <template v-if="!isMyGoods(g)">
                  <el-button
                    v-if="g.price != null && g.price !== ''"
                    size="small"
                    type="primary"
                    plain
                    @click="handleWant(g)"
                  >
                    <el-icon style="margin-right: 4px"><ShoppingCart /></el-icon>
                    我想要
                  </el-button>
                  <el-button
                    size="small"
                    text
                    @click="handleChat(g)"
                  >
                    <el-icon style="margin-right: 4px"><ChatDotRound /></el-icon>
                    私聊我
                  </el-button>
                </template>
                <!-- 本人商品：不再显示购买/私聊按钮，只给出说明文字 -->
                <span v-else class="mine-actions-text">这是你发布的宝贝</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 商品详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      width="720px"
      :close-on-click-modal="false"
      title="商品详情"
    >
      <template v-if="activeGoods">
        <div class="detail-layout">
          <div class="detail-images">
            <img :src="firstImage(activeGoods)" :alt="activeGoods.title" />
          </div>
          <div class="detail-info">
            <h2 class="detail-title">{{ activeGoods.title }}</h2>
            <p class="detail-price">￥{{ formatPrice(activeGoods.price) }}</p>
            <p class="detail-seller">卖家：{{ activeGoods.sellerNickname || '匿名' }}</p>
            <p class="detail-desc">{{ activeGoods.description || '暂无描述' }}</p>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="activeGoods && activeGoods.price != null && activeGoods.price !== ''"
          type="primary"
          :loading="buying"
          @click="handleBuyNow"
        >
          立即购买
        </el-button>
        <el-button v-else-if="activeGoods" disabled>
          暂不售出
        </el-button>
      </template>
    </el-dialog>

    <!-- 发布宝贝弹窗（欢迎页风格，强调图片与信息完整度） -->
    <el-dialog
      v-model="publishVisible"
      width="660px"
      :close-on-click-modal="false"
      class="publish-goods-dialog"
    >
      <template #header>
        <div class="publish-goods-header">
          <div class="publish-goods-icon">
            <el-icon><Picture /></el-icon>
          </div>
          <div class="publish-goods-header-text">
            <div class="publish-goods-title">发布二手宝贝</div>
            <div class="publish-goods-sub">
              精选图片与清晰描述，可以帮助你的宝贝在校园市场中更快找到有缘人。
            </div>
          </div>
        </div>
      </template>
      <div class="publish-goods-body">
        <div class="publish-goods-main">
          <el-form label-position="top" :model="publishForm" class="publish-goods-form">
            <div class="publish-goods-section">
              <div class="section-label">基础信息</div>
              <el-form-item label="标题（必填）">
                <el-input
                  v-model="publishForm.title"
                  maxlength="50"
                  show-word-limit
                  placeholder="例如：九成新蓝牙耳机 / 考研数学真题 / 小型电风扇"
                />
              </el-form-item>
              <el-form-item label="价格（￥，选填，默认暂无报价）">
                <el-input
                  v-model="publishForm.price"
                  type="number"
                  min="0"
                  placeholder="例如：88"
                />
              </el-form-item>
            </div>

            <div class="publish-goods-section">
              <div class="section-label">图片与描述</div>
              <el-form-item label="商品图片">
                <!-- 隐藏的本地图片选择 input，使用自定义按钮触发 -->
                <input
                  ref="uploadImageInputRef"
                  type="file"
                  accept="image/*"
                  multiple
                  style="display: none"
                  @change="handleImageFileChange"
                />
                <div class="publish-goods-upload-row">
                  <el-button type="primary" plain @click="triggerImagePick">
                    <el-icon style="margin-right: 4px"><Picture /></el-icon>
                    选择本地图片
                  </el-button>
                </div>
                <div v-if="imageUploading" class="upload-status">图片上传中，请稍候…</div>
                <div v-if="imageUrls.length" class="upload-preview-list">
                  <div
                    v-for="(url, idx) in imageUrls"
                    :key="url"
                    class="upload-preview-item"
                  >
                    <img :src="url" alt="商品图片预览" />
                    <el-button
                      text
                      type="danger"
                      size="small"
                      class="upload-remove-btn"
                      @click="removeImage(idx)"
                    >
                      移除
                    </el-button>
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="宝贝描述（选填）">
                <el-input
                  v-model="publishForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="可说明成色、购入时间、是否保修、是否包邮/自提等"
                />
              </el-form-item>
            </div>
          </el-form>
        </div>
        <aside class="publish-goods-aside">
          <div class="aside-title">推荐写法</div>
          <ul class="aside-list">
            <li>说明宝贝<strong>成色（如：9 成新）</strong>，是否有明显划痕或损耗。</li>
            <br />
            <li>补充<strong>购买渠道与时间</strong>，提升可信度。</li>
            <br />
            <li>提前注明<strong>交易方式（当面/自提/快递）</strong>及大致地点。</li>
            <br />
            <li>若未想好价格，可点击<strong>一键发布</strong>展览物品</li>
          </ul>
        </aside>
      </div>
      <template #footer>
        <div class="publish-goods-footer">
          <el-button @click="publishVisible = false">取消</el-button>
          <el-button
            type="success"
            :loading="oneClickPublishing"
            @click="handleOneClickPublish"
          >
            一键发布
          </el-button>
          <el-button type="primary" :loading="publishing" @click="handlePublish">
            发布宝贝
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 我的商品抽屉（与“我的任务”相同 Tab 形式） -->
    <el-drawer
      v-model="myGoodsVisible"
      title="我的商品"
      direction="rtl"
      size="480px"
    >
      <el-tabs v-model="myGoodsTab">
        <el-tab-pane label="我买到的" name="bought">
          <p class="my-goods-hint">我购买的宝贝，可查看订单详情并联系卖家</p>
          <el-skeleton v-if="loadingMyGoods" :rows="3" animated />
          <template v-else>
            <el-empty v-if="myBought.length === 0" description="暂时还没有买到任何宝贝" />
            <div v-else class="my-goods-list">
              <div v-for="o in myBought" :key="o.id" class="my-goods-item">
                <div class="my-goods-title">{{ o.goodsTitle || '未知商品' }}</div>
                <div class="my-goods-meta">
                  <span>价格：￥{{ formatPrice(o.price) }}</span>
                  <span>状态：{{ orderStatusText(o.status) }}</span>
                </div>
                <el-button size="small" text type="primary" @click="openOrderGoodsDetail(o)">查看详情</el-button>
              </div>
            </div>
          </template>
        </el-tab-pane>
        <el-tab-pane label="我卖出的" name="sold">
          <p class="my-goods-hint">我售出的宝贝，买家支付后您点击「确认收货」后钱款才到账</p>
          <el-skeleton v-if="loadingMyGoods" :rows="3" animated />
          <template v-else>
            <el-empty v-if="mySold.length === 0" description="暂时还没有卖出任何宝贝" />
            <div v-else class="my-goods-list">
              <div v-for="o in mySold" :key="o.id" class="my-goods-item">
                <div class="my-goods-title">{{ o.goodsTitle || '未知商品' }}</div>
                <div class="my-goods-meta">
                  <span>价格：￥{{ formatPrice(o.price) }}</span>
                  <span>状态：{{ orderStatusText(o.status) }}</span>
                </div>
                <div class="my-goods-item-actions">
                  <el-button
                    v-if="o.status === 'PAID'"
                    type="primary"
                    size="small"
                    :loading="confirmingReceiptId === o.id"
                    @click="handleConfirmReceipt(o)"
                  >
                    确认收货
                  </el-button>
                  <el-button size="small" text type="primary" @click="openOrderGoodsDetail(o)">查看详情</el-button>
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
.goods-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f9fafb, #eef2ff);
  display: flex;
  flex-direction: column;
}

.goods-nav-bar {
  padding: 10px 18px 4px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
}

.goods-nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.goods-nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-nav-logo {
  font-size: 18px;
  color: #2563eb;
}

.goods-nav-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.goods-nav-menu {
  border-bottom: none;
  flex: 1;
  display: flex;
  justify-content: flex-end;
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

.goods-nav-avatar-wrap {
  display: flex;
  align-items: center;
  margin-left: 12px;
}

.goods-nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s ease;
}

.goods-nav-user:hover {
  background: rgba(37, 99, 235, 0.08);
}

.goods-nav-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-nav-avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  object-fit: cover;
  border: 2px solid rgba(37, 99, 235, 0.3);
}

.goods-topbar {
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(14px);
}

.goods-topbar-inner {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 18px;
}

.goods-nav-left .title {
  font-size: 20px;
  font-weight: 800;
  margin: 0;
  color: #111827;
}

.goods-nav-left .subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.goods-nav-center .search-input {
  max-width: 520px;
  margin: 0 auto;
}

.goods-nav-right {
  display: flex;
  gap: 10px;
}

.goods-main {
  flex: 1;
  padding: 18px 22px 24px;
}

.goods-main-inner {
  max-width: 1120px;
  margin: 12px auto 0;
}

.goods-loading,
.goods-empty {
  text-align: center;
  padding: 40px 0;
  color: #6b7280;
}

.goods-empty .desc {
  margin-top: 4px;
  font-size: 13px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.goods-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
}

.image-wrap {
  width: 100%;
  padding-bottom: 70%;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.image-wrap img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 160ms ease;
}

.image-wrap:hover img {
  transform: scale(1.03);
}

.info {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info .title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  cursor: pointer;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
  color: #6b7280;
}

.price {
  font-size: 16px;
  font-weight: 800;
  color: #ef4444;
}

.seller {
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.actions {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 18px;
}

.detail-images img {
  width: 100%;
  border-radius: 16px;
  object-fit: cover;
}

.detail-title {
  font-size: 20px;
  font-weight: 800;
  margin: 0 0 6px;
}

.detail-price {
  font-size: 22px;
  font-weight: 800;
  color: #ef4444;
  margin: 0 0 6px;
}

.detail-seller {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-desc {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

/* 我的商品抽屉（与我的任务风格一致） */
.my-goods-hint {
  font-size: 12px;
  color: #6b7280;
  margin: 0 0 12px;
}

.my-goods-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.my-goods-item {
  padding: 12px;
  border-radius: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.my-goods-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.my-goods-meta {
  font-size: 12px;
  color: #6b7280;
  margin: 6px 0;
  display: flex;
  gap: 12px;
}

.my-goods-item-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

/* 卡片上“我的商品”标记 */
.mine-badge {
  margin-top: 2px;
}

/* 本人商品时替代操作按钮的提示文字 */
.mine-actions-text {
  font-size: 12px;
  color: #9ca3af;
}

/* 卖出宝贝弹窗：图片上传区域的样式 */
.upload-hint {
  display: inline-block;
  margin-left: 10px;
  font-size: 12px;
  color: #6b7280;
}

.upload-status {
  margin-top: 6px;
  font-size: 12px;
  color: #2563eb;
}

.upload-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.upload-preview-item {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-remove-btn {
  position: absolute;
  right: 2px;
  bottom: 2px;
  padding: 0 4px;
  font-size: 11px;
}

/* 发布宝贝弹窗样式（对齐欢迎大厅风格） */
.publish-goods-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 14px 18px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.publish-goods-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.publish-goods-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.16), rgba(129, 140, 248, 0.16));
  color: #0284c7;
}

.publish-goods-header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.publish-goods-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.publish-goods-sub {
  font-size: 12px;
  color: #6b7280;
}

.publish-goods-body {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr);
  gap: 16px;
  padding: 12px 18px 4px;
}

.publish-goods-main {
  min-width: 0;
}

.publish-goods-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.publish-goods-section {
  padding: 10px 12px 12px;
  border-radius: 14px;
  background: #f9fafb;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.publish-goods-upload-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.publish-goods-aside {
  align-self: stretch;
  padding: 10px 12px 12px;
  border-radius: 14px;
  background: #ecfdf3;
  border: 1px solid #4ade80;
  font-size: 12px;
  color: #166534;
}

.publish-goods-footer {
  padding: 8px 18px 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .publish-goods-body {
    grid-template-columns: 1fr;
  }
}

/* 响应式适配：窄屏下让导航和布局更加紧凑 */
@media (max-width: 900px) {
  .goods-nav-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .goods-nav-menu {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .goods-topbar-inner {
    grid-template-columns: 1fr;
    row-gap: 10px;
  }

  .goods-nav-right {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .goods-main {
    padding: 12px 12px 18px;
  }
}
</style>

