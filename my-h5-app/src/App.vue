<template>
  <el-menu
    v-if="showNav"
    ref="navRef"
    mode="horizontal"
    :default-active="route.path"
    router
    :ellipsis="false"
    class="app-nav"
  >
    <router-link to="/home" class="brand">
      <span ref="brandRef" class="brand-inner">
        <img src="/images/nav/yingchao2.png" alt="英超预测" class="brand-logo">
      </span>
    </router-link>

    <el-menu-item
  v-for="(item, i) in navItems"
  :key="item.path"
  :index="item.path"
  class="nav-item"
  :class="{ 'nav-hidden': !isMobile && i >= visibleCount }"
  :ref="(el: unknown) => setItemRef(el, i)"
>
  <img
    v-if="route.path === item.path"
    :src="item.icon"
    :alt="item.label"
    class="nav-icon"
  >
  <template v-else>{{ item.label }}</template>
</el-menu-item>

    <!-- 「更多」：只在非手机端显示（手机上横向滚动看全部） -->
    <el-dropdown
      v-if="!isMobile && overflowItems.length > 0"
      class="nav-more"
      trigger="click"
      @command="(p: string) => router.push(p)"
    >
      <span class="nav-more-trigger" :class="{ 'is-active': activeInOverflow }">
        <el-icon><MoreFilled /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="item in overflowItems"
            :key="item.path"
            :command="item.path"
            :class="{ 'is-current': item.path === route.path }"
          >
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 右侧登录态 -->
    <div ref="rightRef" class="nav-right">
      <el-dropdown v-if="userStore.isLoggedIn" @command="onCommand">
        <span class="user-trigger">
          <el-avatar :size="28" :src="avatarImgSrc" v-if="avatarImgSrc" />
          <el-avatar :size="28" v-else>{{ avatarText }}</el-avatar>
          <span class="uname">{{ displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">用户中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button v-else type="primary" size="small" @click="router.push('/login')">未登录</el-button>
    </div>
  </el-menu>

  <div class="app-main">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, MoreFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { avatarSrc } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showNav = computed(() => route.meta.nav !== false)

const displayName = computed(
  () => userStore.profile?.nickname || userStore.profile?.username || '已登录'
)
const avatarText = computed(() => (displayName.value || '?').charAt(0).toUpperCase())
const avatarImgSrc = computed(() => avatarSrc(userStore.profile?.avatarUrl))

onMounted(() => {
  userStore.restore()
})

function onCommand(command: string) {
  if (command === 'profile') {
    router.push('/user')
  } else if (command === 'logout') {
    userStore.logout()
    router.replace('/login')
  }
}

/* ───────────────────────────────────────────────────────────
   窄屏折叠逻辑（仅非手机端用）
   ─────────────────────────────────────────────────────────── */

const navItems = [
  { path: '/home', label: '预测', icon: '/title/yuce.png' },
  { path: '/highlights', label: '精彩时刻', icon: '/title/jingcai.png' },
  { path: '/standings', label: '积分榜', icon: '/title/jifen.png' },
  { path: '/history', label: '历史回顾', icon: '/title/lishi.png' }
]

const MORE_WIDTH = 44
const SAFE_PAD = 8

const navRef = ref()
const brandRef = ref<HTMLElement>()
const rightRef = ref<HTMLElement>()
const itemEls: (HTMLElement | null)[] = []
function setItemRef(el: unknown, i: number) {
  const dom = (el as { $el?: HTMLElement } | null)?.$el
  itemEls[i] = dom instanceof HTMLElement ? dom : null
}

const visibleCount = ref(navItems.length)
const cachedWidths: number[] = []

/** 是否是手机端（<= 640px） */
const isMobile = ref(false)

const overflowItems = computed(() => navItems.slice(visibleCount.value))
const activeInOverflow = computed(() => overflowItems.value.some((i) => i.path === route.path))

function measureItemWidths() {
  itemEls.forEach((el, i) => {
    if (!el) return
    const w = el.getBoundingClientRect().width
    if (w > 0.5) cachedWidths[i] = w
  })
}

function updateVisibleCount() {
  // 手机端：不折叠，全部显示，靠横向滚动
  isMobile.value = window.innerWidth <= 640
  if (isMobile.value) {
    visibleCount.value = navItems.length
    return
  }

  const nav = navRef.value?.$el as HTMLElement | undefined
  if (!nav) return
  const total = nav.getBoundingClientRect().width
  const brandW = brandRef.value?.getBoundingClientRect().width ?? 0
  const rightW = rightRef.value?.getBoundingClientRect().width ?? 0
  const available = total - brandW - rightW - SAFE_PAD

  let used = 0
  let count = 0
  for (let i = 0; i < cachedWidths.length; i++) {
    const isLastItem = i === cachedWidths.length - 1
    const reserve = isLastItem ? 0 : MORE_WIDTH
    if (used + (cachedWidths[i] ?? 0) + reserve <= available) {
      used += cachedWidths[i] ?? 0
      count++
    } else {
      break
    }
  }
  visibleCount.value = Math.min(Math.max(count, 1), navItems.length)
}

let resizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await nextTick()
  updateVisibleCount()
  measureItemWidths()
  updateVisibleCount()

  document.fonts?.ready?.then(() => {
    measureItemWidths()
    updateVisibleCount()
  })

  const nav = navRef.value?.$el
  if (nav && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => updateVisibleCount())
    resizeObserver.observe(nav)
  }
  window.addEventListener('resize', updateVisibleCount)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', updateVisibleCount)
})
</script>

<style scoped>
.app-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #ffffff;
  flex-wrap: nowrap;
}
/* 去掉当前页的蓝色文字和底部横线 */
.app-nav :deep(.el-menu-item.is-active) {
  color: inherit;           /* 文字颜色不变 */
  border-bottom: none;      /* 去掉底部横线 */
  background-color: transparent;  /* 去掉背景色 */
}

/* 悬停也不要蓝底 */
.app-nav :deep(.el-menu-item:hover) {
  background-color: transparent;
}

/* 去掉 el-menu 整体的底部横线 */
.app-nav :deep(.el-menu) {
  border-bottom: none;
}

/* 去掉 el-menu-item 的激活下划线 */
.app-nav :deep(.el-menu-item) {
  border-bottom: none;
}
/* 参与测量的节点不压缩 */
.brand,
.nav-right,
.nav-more,
.nav-item {
  flex-shrink: 0;
}
.nav-hidden {
  display: none;
}

.brand {
  font-weight: 700;
  font-size: 18px;
  padding: 16px 16px;
  white-space: nowrap;
}
.brand-inner {
  display: inline-flex;
  align-items: center;
}
.brand-logo {
  height: 40px;
  width: auto;
  display: block;
}

.nav-icon {
  height: 50px;      /* 根据导航栏高度调 */
  width: auto;
  display: block;
}
.nav-more {
  display: flex;
  align-items: center;
}
.nav-more-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  outline: none;
  color: var(--el-text-color-regular);
}
.nav-more-trigger:hover,
.nav-more-trigger.is-active {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
.nav-more :deep(.el-dropdown-menu__item.is-current) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  padding-right: 12px;
}
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}
.uname {
  font-size: 14px;
}

/* ── 手机端：横向滚动 ── */
@media (max-width: 640px) {
  .app-nav {
    --el-menu-base-level-padding: 12px;
    --el-menu-horizontal-height: 52px;
    overflow-x: auto;       /* 横向滚动 */
    overflow-y: hidden;
    flex-wrap: nowrap;
    scrollbar-width: none;  
    font-family: "SimSun", "宋体", serif;   /* ← 加这行 */
  font-weight: bold;/* 隐藏滚动条（Firefox） */
  }
  .app-nav::-webkit-scrollbar {
    display: none;          /* 隐藏滚动条（Chrome/Safari） */
  }
  .brand {
    padding: 14px 10px;
  }
  .brand-logo {
    height: 32px;
  }
  .uname {
    max-width: 84px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>