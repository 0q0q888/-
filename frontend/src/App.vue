<script setup>
/**
 * 根组件。
 * 说明：
 * - 负责渲染 <router-view />，真正的页面内容由各个路由组件提供：
 *   - /login    -> Login.vue；
 *   - /register -> Register.vue；
 *   - /home     -> Home.vue；
 *   - /tasks    -> Tasks.vue 等。
 * - 通过 <Transition> 实现路由切换的丝滑过渡效果（例如：登录页 -> 注册页）。
 * - 在除登录页以外的页面统一挂载右侧悬浮快捷栏 FloatingActions。
 */
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import FloatingActions from './components/FloatingActions.vue'

const route = useRoute()

const shouldShowFloating = computed(() => route.path !== '/login')
</script>

<template>
  <!--
    使用插槽拿到当前路由对应的组件，然后包一层 Transition 做转场动画。
    mode="out-in"：先让旧页面离场，再让新页面进场，视觉更“丝滑”。
  -->
  <RouterView v-slot="{ Component, route }">
    <Transition name="route" mode="out-in">
      <component :is="Component" :key="route.path" />
    </Transition>
  </RouterView>

  <!-- 全局右侧悬浮栏：登录页不显示，其余页面统一显示 -->
  <FloatingActions v-if="shouldShowFloating" />
</template>

<style scoped>
/**
 * 路由切换动画：
 * - 进场：轻微上移 + 渐显；
 * - 离场：轻微下移 + 渐隐；
 * 说明：幅度和时长都保持克制，更像商业产品而不是“花哨特效”。
 */
.route-enter-active,
.route-leave-active {
  transition:
    opacity 220ms ease,
    transform 220ms ease;
}

.route-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.route-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (prefers-reduced-motion: reduce) {
  .route-enter-active,
  .route-leave-active {
    transition: none;
  }
}
</style>
