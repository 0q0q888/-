import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import Tasks from '../views/Tasks.vue'
import Profile from '../views/Profile.vue'
import Messages from '../views/Messages.vue'
import GoodsMarket from '../views/GoodsMarket.vue'
import AiCustomerService from '../views/AiCustomerService.vue'

/**
 * 应用路由配置。
 */
const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: Tasks,
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
  },
  {
    path: '/market',
    name: 'GoodsMarket',
    component: GoodsMarket,
  },
  {
    path: '/messages',
    name: 'Messages',
    component: Messages,
  },
  {
    path: '/ai-customer-service',
    name: 'AiCustomerService',
    component: AiCustomerService,
  },
]

/**
 * 创建并导出全局路由实例。
 * 说明：
 * - 使用 HTML5 history 模式（createWebHistory），URL 更简洁。
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

