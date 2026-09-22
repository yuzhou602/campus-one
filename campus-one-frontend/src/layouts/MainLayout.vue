<template>
  <div class="flex h-dvh overflow-hidden">
    <button
      v-if="mobileOpen"
      type="button"
      class="fixed inset-0 z-30 bg-ink-900/35 lg:hidden"
      aria-label="关闭导航菜单"
      @click="mobileOpen = false"
    />
    <!-- ===== 左侧 · 档案卷宗书脊 ===== -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-40 flex flex-col shrink-0 border-r border-line bg-side face transition-transform lg:relative lg:z-auto lg:translate-x-0',
        mobileOpen ? 'translate-x-0' : '-translate-x-full',
        appStore.sidebarCollapsed ? 'w-60 lg:w-16' : 'w-60',
      ]"
    >
      <!-- 书脊装订（左侧细墨带） -->
      <span class="absolute left-0 top-0 bottom-0 w-1 bg-ink-900/25" />

      <!-- 卷宗封面 / Logo -->
      <div class="flex items-center h-16 px-4 border-b border-line">
        <div class="flex items-center gap-2.5 overflow-hidden">
          <div
            class="w-8 h-8 bg-ink-900 text-white flex items-center justify-center text-[13px] font-semibold tracking-tight shrink-0"
            style="border-radius: 3px"
          >
            壹
          </div>
          <div v-if="mobileOpen || !appStore.sidebarCollapsed" class="min-w-0">
            <div class="text-[13px] font-semibold text-ink-900 leading-tight truncate">校园档案</div>
            <div class="text-[10px] text-ink-300 tracking-[0.2em] font-mono uppercase">Vol.01 · 智慧校园</div>
          </div>
        </div>
      </div>

      <!-- 卷宗目录（导航=档案目录条目） -->
      <nav class="flex-1 py-5 overflow-y-auto px-2">
        <div v-for="(group, gi) in menuGroups" :key="group.label">
          <div
            v-if="(mobileOpen || !appStore.sidebarCollapsed) && group.label"
            class="arch-sec px-2 mb-2 mt-4 text-[10px]"
            :class="{ 'mt-0': gi === 0 }"
          >
            {{ group.label }}
          </div>

          <div v-for="(item, ii) in group.items" :key="item.path">
            <router-link
              v-if="hasAccess(item)"
              :to="item.path"
              :class="[
                'nav-entry group/item',
                isActive(item.path) ? 'active' : '',
              ]"
              :title="item.title"
              @click="mobileOpen = false"
            >
              <span v-if="mobileOpen || !appStore.sidebarCollapsed" class="nav-no">{{ pad(catalogNo(group, ii)) }}</span>
              <el-icon :size="18" class="shrink-0">
                <component :is="item.icon" />
              </el-icon>
              <span v-if="mobileOpen || !appStore.sidebarCollapsed" class="whitespace-nowrap nav-label">{{ item.title }}</span>
              <span v-if="(mobileOpen || !appStore.sidebarCollapsed) && isActive(item.path)" class="nav-dot" />
            </router-link>
          </div>
        </div>
      </nav>

      <!-- 卷宗末尾 · 收起 -->
      <div class="p-3 border-t border-line">
        <button
          type="button"
          @click="appStore.toggleSidebar()"
          class="w-full flex items-center justify-center gap-2 px-3 py-2 text-[13px] text-ink-500 hover:text-ink-900 hover:bg-ink-900/5 rounded-md transition-colors"
          style="border-radius: 6px"
        >
          <el-icon :size="16">
            <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <span v-if="mobileOpen || !appStore.sidebarCollapsed" class="text-sm">收起</span>
        </button>
        <p v-if="mobileOpen || !appStore.sidebarCollapsed" class="text-center text-[10px] text-ink-300 mt-3 font-mono">
          档 · CN/2026-18
        </p>
      </div>
    </aside>

    <!-- ===== 主区域 ===== -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- 顶栏：细墨线 -->
      <header class="h-16 bg-surface/90 backdrop-blur-sm border-b border-line flex items-center justify-between px-3 sm:px-6 shrink-0 relative">
        <div class="flex min-w-0 items-center gap-2">
          <button type="button" class="p-2 rounded-md text-ink-700 hover:bg-ink-900/5 lg:hidden" aria-label="打开导航菜单" @click="mobileOpen = true">
            <el-icon :size="20"><Menu /></el-icon>
          </button>
        <el-breadcrumb separator="/" class="hidden min-w-0 sm:flex">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <span class="truncate text-sm font-medium text-ink-900 sm:hidden">{{ route.meta.title || '智慧校园' }}</span>
        </div>

        <div class="flex items-center gap-3">
          <!-- 快速办理 -->
          <el-dropdown trigger="click">
            <button type="button" aria-label="快速办理" class="flex items-center gap-1.5 px-2 sm:px-3 py-1.5 text-[13px] text-ink-700 hover:text-ink-900 hover:bg-ink-900/5 rounded-md transition-colors">
              <el-icon :size="15"><Plus /></el-icon>
              <span class="hidden sm:inline">快速办理</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/service/apply/1')">
                  <el-icon><Edit /></el-icon>请假申请
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/reservation')">
                  <el-icon><Calendar /></el-icon>场地预约
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/repair')">
                  <el-icon><Tools /></el-icon>校园报修
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/service/apply/2')">
                  <el-icon><Document /></el-icon>证明申请
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- AI 助手 -->
          <el-tooltip content="AI 校园助手" placement="bottom">
            <button
              type="button"
              aria-label="打开 AI 校园助手"
              @click="$router.push('/ai')"
              class="p-2 rounded-md text-ink-700 hover:text-ink-900 hover:bg-ink-900/5 transition-colors"
            >
              <el-icon :size="19"><MagicStick /></el-icon>
            </button>
          </el-tooltip>

          <!-- 消息 -->
          <el-badge :value="notificationStore.unreadCount" :hidden="!notificationStore.unreadCount" :max="99">
            <el-tooltip content="消息中心" placement="bottom">
              <button
                type="button"
                aria-label="打开消息中心"
                @click="$router.push('/message')"
                class="p-2 rounded-md text-ink-700 hover:text-ink-900 hover:bg-ink-900/5 transition-colors"
              >
                <el-icon :size="19"><Bell /></el-icon>
              </button>
            </el-tooltip>
          </el-badge>

          <!-- 用户 -->
          <el-dropdown trigger="click">
            <div class="flex items-center gap-2.5 cursor-pointer px-2 py-1 rounded-md hover:bg-ink-900/5 transition-colors">
              <div class="w-8 h-8 bg-ink-900 text-white flex items-center justify-center text-[13px] font-semibold" style="border-radius: 3px">
                {{ userStore.realName?.charAt(0) || '问' }}
              </div>
              <div v-if="!appStore.sidebarCollapsed" class="hidden sm:block text-left">
                <div class="text-[13px] font-medium text-ink-900 leading-tight">{{ userStore.realName }}</div>
                <div class="text-[11px] text-ink-500 leading-tight">{{ roleText }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容 -->
      <main class="flex-1 overflow-y-auto p-3 sm:p-6 relative">
        <div v-if="isDemoMode" class="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-ink-900/15 bg-surface px-4 py-2 text-xs text-ink-700">
          <span><strong>在线演示模式</strong> · 所有数据均为虚构，操作仅保存在当前浏览器。</span>
          <button type="button" class="font-medium underline underline-offset-2" @click="resetDemoData">重置演示数据</button>
        </div>
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { isDemoMode } from '@/demo'
import {
  HomeFilled, Service, Document, Stamp, Calendar, Tools, Flag, Bell,
  ChatDotRound, MagicStick, List, DataAnalysis, Setting, Plus, Edit,
  SwitchButton,
  Menu,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const mobileOpen = ref(false)

function resetDemoData() {
  Object.keys(localStorage)
    .filter(key => key.startsWith('campusone-demo-') && key !== 'campusone-demo-current-user')
    .forEach(key => localStorage.removeItem(key))
  window.location.reload()
}

watch(() => route.fullPath, () => { mobileOpen.value = false })

const menuGroups = computed(() => {
  const allGroups = [
    {
      label: '',
      items: [{ path: '/dashboard', title: '智慧首页', icon: HomeFilled }],
    },
    {
      label: '校园服务',
      items: [
        { path: '/service', title: '校园事务', icon: Service },
        { path: '/reservation', title: '场地预约', icon: Calendar },
        { path: '/repair', title: '校园报修', icon: Tools },
      ],
    },
    {
      label: '校园生活',
      items: [
        { path: '/activity', title: '校园活动', icon: Flag },
        { path: '/notice', title: '校园资讯', icon: Bell },
      ],
    },
    {
      label: '个人中心',
      items: [
        { path: '/application/my', title: '我的申请', icon: Document },
        { path: '/task', title: '任务中心', icon: List },
      ],
    },
    {
      label: '智能服务',
      items: [
        { path: '/ai', title: 'AI校园助手', icon: MagicStick },
        { path: '/message', title: '消息中心', icon: ChatDotRound },
      ],
    },
    {
      label: '管理',
      items: [
        { path: '/approval', title: '审批中心', icon: Stamp, roles: ['TEACHER', 'COUNSELOR', 'ADMIN', 'SUPER_ADMIN'] },
        { path: '/analytics', title: '数据中心', icon: DataAnalysis, roles: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/system', title: '系统管理', icon: Setting, roles: ['SUPER_ADMIN'] },
      ],
    },
  ]
  return allGroups
})

function hasAccess(item: any) {
  if (!item.roles) return true
  return item.roles.includes(userStore.role)
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function catalogNo(group: any, index: number) {
  const gi = menuGroups.value.indexOf(group)
  return gi * 10 + index + 1
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

const roleText = computed(() => {
  const m: Record<string, string> = {
    SUPER_ADMIN: '超级管理员', ADMIN: '管理员',
    TEACHER: '教师', COUNSELOR: '职工',
    STUDENT: '学生',
  }
  return m[userStore.role] || '成员'
})

function handleLogout() {
  userStore.logout()
  notificationStore.stopPolling()
  router.push('/login')
}

onMounted(() => {
  notificationStore.startPolling()
})

onUnmounted(() => {
  notificationStore.stopPolling()
})
</script>

<style scoped>
/* 书脊侧栏纸色 */
.bg-side {
  background: #EAE2D3;
}

/* 目录条目 */
.nav-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin: 2px 4px;
  border-radius: 6px;
  color: var(--color-ink-700);
  font-size: 13px;
  transition: background-color 0.15s ease-out, color 0.15s ease-out;
  position: relative;
}
.nav-entry:hover {
  background: rgb(34 29 24 / 0.06);
  color: var(--color-ink-900);
}
.nav-entry.active {
  background: var(--color-ink-900);
  color: #fff;
  box-shadow: 0 1px 3px rgb(34 29 24 / 0.2);
}
.nav-entry .nav-no {
  font-family: ui-monospace, monospace;
  font-size: 10px;
  color: var(--color-ink-300);
  width: 16px;
  letter-spacing: 0.05em;
}
.nav-entry.active .nav-no {
  color: rgb(255 255 255 / 0.55);
}
.nav-entry .nav-label {
  flex: 1;
}
.nav-entry .nav-dot {
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: #fff;
  opacity: 0.9;
}

.page-fade-enter-active {
  transition: opacity 0.15s ease;
}
.page-fade-leave-active {
  transition: opacity 0.1s ease;
}
.page-fade-enter-from, .page-fade-leave-to {
  opacity: 0;
}
</style>
