import { createRouter, createWebHashHistory, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'
import { isDemoMode } from '@/demo'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '智慧首页', icon: 'HomeFilled' },
      },
      {
        path: 'service',
        name: 'Service',
        component: () => import('@/views/service/ServiceCenter.vue'),
        meta: { title: '校园事务', icon: 'Service' },
      },
      {
        path: 'service/apply/:serviceId',
        name: 'ServiceApply',
        component: () => import('@/views/service/ServiceApply.vue'),
        meta: { title: '事务申请', hidden: true },
      },
      {
        path: 'application/my',
        name: 'MyApplications',
        component: () => import('@/views/application/MyApplications.vue'),
        meta: { title: '我的申请', icon: 'Document' },
      },
      {
        path: 'application/detail/:id',
        name: 'ApplicationDetail',
        component: () => import('@/views/application/ApplicationDetail.vue'),
        meta: { title: '申请详情', hidden: true },
      },
      {
        path: 'approval',
        name: 'ApprovalCenter',
        component: () => import('@/views/approval/ApprovalCenter.vue'),
        meta: { title: '审批中心', icon: 'Stamp', roles: ['TEACHER', 'COUNSELOR', 'ADMIN', 'SUPER_ADMIN'] },
      },
      {
        path: 'reservation',
        name: 'Reservation',
        component: () => import('@/views/reservation/ReservationList.vue'),
        meta: { title: '场地预约', icon: 'Calendar' },
      },
      {
        path: 'reservation/detail/:id',
        name: 'ReservationDetail',
        component: () => import('@/views/reservation/ReservationDetail.vue'),
        meta: { title: '场地详情', hidden: true },
      },
      {
        path: 'repair',
        name: 'Repair',
        component: () => import('@/views/repair/RepairList.vue'),
        meta: { title: '校园报修', icon: 'Tools' },
      },
      {
        path: 'repair/detail/:id',
        name: 'RepairDetail',
        component: () => import('@/views/repair/RepairDetail.vue'),
        meta: { title: '报修详情', hidden: true },
      },
      {
        path: 'repair/my',
        name: 'MyRepairs',
        component: () => import('@/views/repair/MyRepairs.vue'),
        meta: { title: '我的报修', hidden: true },
      },
      {
        path: 'activity',
        name: 'Activity',
        component: () => import('@/views/activity/ActivityList.vue'),
        meta: { title: '校园活动', icon: 'Flag' },
      },
      {
        path: 'activity/detail/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/activity/ActivityDetail.vue'),
        meta: { title: '活动详情', hidden: true },
      },
      {
        path: 'notice',
        name: 'Notice',
        component: () => import('@/views/notice/NoticeList.vue'),
        meta: { title: '校园资讯', icon: 'Bell' },
      },
      {
        path: 'notice/detail/:id',
        name: 'NoticeDetail',
        component: () => import('@/views/notice/NoticeDetail.vue'),
        meta: { title: '通知详情', hidden: true },
      },
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/message/MessageCenter.vue'),
        meta: { title: '消息中心', icon: 'ChatDotRound' },
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/AICopilot.vue'),
        meta: { title: 'AI校园助手', icon: 'MagicStick' },
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/TaskCenter.vue'),
        meta: { title: '任务中心', icon: 'List' },
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/analytics/DataAnalytics.vue'),
        meta: { title: '数据中心', icon: 'DataAnalysis', roles: ['ADMIN', 'SUPER_ADMIN'] },
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/SystemManagement.vue'),
        meta: { title: '系统管理', icon: 'Setting', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/SystemUsers.vue'),
        meta: { title: '用户管理', roles: ['SUPER_ADMIN'], hidden: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: isDemoMode ? createWebHashHistory(import.meta.env.BASE_URL) : createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || ''} - CampusOne`

  const userStore = useUserStore()

  // Auto-fetch user info on first navigation after page refresh
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.initOnAppStart()
  }

  if (to.meta.requiresAuth === false) {
    next()
  } else if (!userStore.isLoggedIn) {
    next('/login')
  } else {
    if (to.meta.roles && Array.isArray(to.meta.roles)) {
      const allowed = (to.meta.roles as string[]).includes(userStore.role)
      if (allowed) {
        next()
      } else {
        next('/dashboard')
      }
    } else {
      next()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
