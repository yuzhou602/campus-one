<template>
  <div class="space-y-6 relative">
    <!-- 登记簿页眉 -->
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <div class="arch-title">{{ greeting }}，{{ userStore.realName }}</div>
          <p class="arch-sub mt-1">
            今日 {{ today }} · {{ weekDay }}
            <span class="text-ink-300">·</span>
            <span class="text-ink-500">{{ roleText }}</span>
          </p>
        </div>
        <span class="arch-mark n-rec">{{ isAdmin ? 'ACCOUNT' : 'REC' }} · {{ today }}</span>
      </div>
    </div>

    <!-- ===== 管理员 / 管理视角：账户与系统管理为主 ===== -->
    <template v-if="isAdmin">
      <!-- 关键运营指标 -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div v-for="(stat, i) in adminStats" :key="stat.label" class="sheet p-5">
          <div class="flex items-center justify-between">
            <span class="text-xs text-ink-500">{{ stat.label }}</span>
            <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
          </div>
          <div class="text-3xl font-semibold text-ink-900 mt-2 tabular-nums">{{ stat.value }}</div>
          <div class="text-xs mt-1 text-ink-300">{{ stat.hint }}</div>
        </div>
      </div>

      <!-- 管理模块入口 -->
      <div class="sheet p-6">
        <div class="arch-sec mb-4">系统管理 · 进入档案</div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <router-link
            v-for="(m, i) in adminModules"
            :key="m.path"
            :to="m.path"
            class="border-2 border-ink-900/10 hover:border-ink-900/50 rounded-lg p-4 bg-surface transition-colors"
            style="border-radius: 8px"
          >
            <div class="flex items-center justify-between">
              <el-icon :size="20" class="text-ink-700"><component :is="m.icon" /></el-icon>
              <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
            </div>
            <div class="text-sm font-medium text-ink-900 mt-3">{{ m.title }}</div>
            <div class="text-[11px] text-ink-300 mt-1">前往管理 →</div>
          </router-link>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 在途审批 -->
        <div class="lg:col-span-2 sheet p-6">
          <div class="flex items-center justify-between mb-4">
            <div class="arch-sec">在途审批<span v-if="pendingCount" class="n-rec text-[11px] text-ink-500 ml-2">{{ pendingCount }} 卷</span></div>
            <router-link to="/approval" class="text-xs text-ink-700 hover:text-ink-900 hover:underline">进入审批中心 →</router-link>
          </div>
          <div v-if="pendingApprovals.length" class="divide-y divide-line/70">
            <div v-for="(item, i) in pendingApprovals" :key="item.id" class="flex items-center gap-4 py-3">
              <span class="n-rec text-[10px] text-ink-300 w-6">{{ pad(i + 1) }}</span>
              <div class="flex-1 min-w-0">
                <div class="text-[13px] font-medium text-ink-900 truncate">
                  {{ item.applicationNo || '待审批申请' }}
                  <span v-if="overdueDays(item) > 0" class="stamp-line text-[10px] ml-1.5">滞压 {{ overdueDays(item) }} 天</span>
                </div>
                <div class="text-[11px] text-ink-500 mt-0.5">
                  {{ item.applicantName || '申请人 #' + item.applicantId }} · {{ item.currentNode || '审批节点' }}
                </div>
              </div>
              <el-button size="small" text @click="$router.push(`/application/detail/${item.id}`)">审阅 →</el-button>
            </div>
          </div>
          <el-empty v-else description="无在途审批，可稍作歇息" :image-size="72" />
        </div>

        <!-- 运行信息 -->
        <div class="sheet p-6">
          <div class="arch-sec mb-4">运行状态</div>
          <div class="space-y-2.5">
            <div v-for="inf in sysInfo" :key="inf.k" class="flex items-center justify-between px-3 py-2 border border-line rounded-md">
              <span class="text-[11px] text-ink-500">{{ inf.k }}</span>
              <span class="text-[12px] text-ink-900 n-rec truncate max-w-[55%]">{{ inf.v || '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 学生 / 教师 / 辅导员 视角 ===== -->
    <template v-else>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 space-y-6">
          <!-- 今日课程（学生） -->
          <div v-if="userStore.role === 'STUDENT'" class="sheet p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="arch-sec">今日课程</div>
              <span class="n-rec text-xs">{{ todayCourses.length }} 节</span>
            </div>
            <div v-if="todayCourses.length" class="divide-y divide-line/70">
              <div v-for="course in todayCourses" :key="course.id" class="flex items-center gap-4 py-3 first:pt-0 last:pb-0">
                <div class="text-center shrink-0 border-r border-line pr-4">
                  <div class="text-[15px] font-semibold text-ink-900 tabular-nums">{{ course.startTime }}</div>
                  <div class="text-[11px] text-ink-300">{{ course.endTime }}</div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-[14px] font-medium text-ink-900 truncate">{{ course.name }}</div>
                  <div class="text-xs text-ink-500 mt-0.5">{{ course.location }} · {{ course.teacher }}</div>
                </div>
                <span class="stamp stamp-line text-[10px]">第{{ course.week }}周</span>
              </div>
            </div>
            <el-empty v-else description="今日无课程，给自己留一点从容。" :image-size="72" />
          </div>

          <!-- 我的待办 -->
          <div class="sheet p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="arch-sec">我的待办</div>
              <router-link to="/task" class="text-xs text-ink-700 hover:text-ink-900 hover:underline">查看全部 →</router-link>
            </div>
            <div class="divide-y divide-line/70">
              <div v-for="task in pendingTasks" :key="task.title" class="flex items-center gap-3 py-3">
                <span class="w-2 h-2 rounded-full bg-ink-900 shrink-0" />
                <div class="text-sm font-medium text-ink-900">{{ task.title }}</div>
                <span class="ml-auto text-xs n-rec shrink-0">{{ task.count }} 件待处理</span>
              </div>
            </div>
            <el-empty v-if="!pendingTasks.length" description="暂无待办，账目清爽。" :image-size="72" />
          </div>

          <!-- 待我审批（教师/辅导员） -->
          <div v-if="isApprover" class="sheet p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="arch-sec">待我审批</div>
              <router-link to="/approval" class="text-xs text-ink-700 hover:text-ink-900 hover:underline">进入审批中心 →</router-link>
            </div>
            <div v-if="pendingApprovals.length" class="divide-y divide-line/70">
              <div v-for="(item, i) in pendingApprovals" :key="item.id" class="flex items-center gap-4 py-3">
                <span class="n-rec text-[10px] text-ink-300 w-6">{{ pad(i + 1) }}</span>
                <div class="flex-1 min-w-0">
                  <div class="text-[13px] font-medium text-ink-900 truncate">{{ item.applicationNo || '待审批申请' }}</div>
                  <div class="text-[11px] text-ink-500 mt-0.5">{{ item.applicantName || '申请人 #' + item.applicantId }}</div>
                </div>
                <span class="stamp text-[10px]">待审</span>
              </div>
            </div>
            <el-empty v-else description="无待审卷宗" :image-size="72" />
          </div>
        </div>

        <!-- 右列：通知 / 活动 -->
        <div class="space-y-6">
          <div class="sheet p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="arch-sec">校园通知</div>
              <router-link to="/notice" class="text-xs text-ink-700 hover:text-ink-900 hover:underline">更多 →</router-link>
            </div>
            <div v-if="recentNotices.length" class="space-y-3">
              <div v-for="notice in recentNotices" :key="notice.id" class="flex items-start gap-3 p-2 rounded-md hover:bg-ink-900/4 transition-colors cursor-pointer"
                @click="$router.push(`/notice/detail/${notice.id}`)">
                <span :class="['w-1.5 h-1.5 rounded-full mt-1.5 shrink-0', notice.unread ? 'bg-ink-900' : 'bg-ink-300/50']" />
                <div class="flex-1 min-w-0">
                  <div class="text-[13px] text-ink-900 truncate">{{ notice.title }}</div>
                  <div class="text-[11px] text-ink-300 mt-0.5">{{ notice.time }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无通知" :image-size="72" />
          </div>

          <div class="sheet p-6">
            <div class="flex items-center justify-between mb-4">
              <div class="arch-sec">推荐活动</div>
              <router-link to="/activity" class="text-xs text-ink-700 hover:text-ink-900 hover:underline">更多 →</router-link>
            </div>
            <div v-if="recommendedActivities.length" class="space-y-3">
              <div v-for="activity in recommendedActivities" :key="activity.id" class="p-3 border border-line rounded-md bg-surface hover:border-ink-500 transition-colors cursor-pointer"
                @click="$router.push(`/activity/detail/${activity.id}`)">
                <div class="text-sm font-medium text-ink-900">{{ activity.title }}</div>
                <div class="flex items-center gap-2 mt-1.5 text-xs text-ink-500">
                  <span>{{ activity.date }}</span><span>·</span><span>{{ activity.location }}</span>
                </div>
                <div class="mt-2.5">
                  <el-progress :percentage="Math.round((activity.registered / activity.capacity) * 100)" :stroke-width="4" color="#221D18" />
                  <div class="text-[11px] text-ink-500 mt-1 n-rec">{{ activity.registered }}/{{ activity.capacity }} 人已领档</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无推荐活动" :image-size="72" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getStudentDashboard, getTeacherDashboard, getAnalyticsOverview } from '@/api/dashboard'
import { getNotices } from '@/api/notice'
import { getActivities } from '@/api/activity'
import { getPendingApprovals, getPendingApprovalsCount } from '@/api/application'
import { getSystemInfo } from '@/api/system'
import { User, Setting, Stamp, DataAnalysis } from '@element-plus/icons-vue'

const userStore = useUserStore()

const today = dayjs().format('M月D日')
const weekDay = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][dayjs().day()]

const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userStore.role))
const isApprover = computed(() => ['TEACHER', 'COUNSELOR', 'ADMIN', 'SUPER_ADMIN'].includes(userStore.role))

const roleText = computed(() => {
  const m: Record<string, string> = {
    SUPER_ADMIN: '超级管理员', ADMIN: '管理员',
    TEACHER: '教师', COUNSELOR: '职工',
    STUDENT: '学生',
  }
  return m[userStore.role] || '成员'
})

const greeting = computed(() => {
  const h = dayjs().hour()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

function pad(n: number) {
  return String(n).padStart(2, '0')
}

/** 超时提醒：处理中的卷宗停留超过阈值天数标记为滞压 */
function overdueDays(item: any): number {
  if (item.status !== 'PENDING') return 0
  const start = item.submittedAt || item.createdAt
  if (!start) return 0
  const days = Math.floor((Date.now() - new Date(start).getTime()) / 86400000)
  return Math.max(days - OVERDUE_DAYS, 0)
}

/* —— 管理视角数据 —— */
const adminStats = ref<{ label: string; value: string | number; hint: string }[]>([])
const adminModules = [
  { title: '用户管理', path: '/system/users', icon: User },
  { title: '系统管理', path: '/system', icon: Setting },
  { title: '审批中心', path: '/approval', icon: Stamp },
  { title: '数据中心', path: '/analytics', icon: DataAnalysis },
]
const pendingApprovals = ref<any[]>([])
const pendingCount = ref(0)
const OVERDUE_DAYS = 3
const sysInfo = ref<{ k: string; v: string }[]>([])

/* —— 校园视角数据 —— */
const todayCourses = ref<any[]>([])
const pendingTasks = ref<any[]>([])
const recentNotices = ref<any[]>([])
const recommendedActivities = ref<any[]>([])

function reportDashboardError(section: string, error: unknown) {
  console.error(`加载${section}失败`, error)
}

onMounted(async () => {
  const role = userStore.role

  if (isAdmin.value) {
    // 管理视角
    try {
      const o = (await getAnalyticsOverview()).data || {}
      adminStats.value = [
        { label: '在案用户', value: o.totalUsers ?? 0, hint: '平台用户总数' },
        { label: '今日预约', value: o.todayReservations ?? 0, hint: '场地面今日排期' },
        { label: '待处理工单', value: o.pendingRepairs ?? 0, hint: '报修中待受理' },
        { label: '活动在档', value: o.totalActivities ?? 0, hint: '校园活动登记数' },
      ]
    } catch (error) { reportDashboardError('管理数据总览', error) }
    loadPendingApprovals()
    try {
      const info = (await getSystemInfo()).data || {}
      sysInfo.value = [
        { k: '版本', v: info.version }, { k: '平台', v: info.name },
        { k: '运行时', v: info.javaVersion }, { k: '系统', v: info.osName },
      ]
    } catch (error) { reportDashboardError('系统信息', error) }
    return
  }

  // 校园视角
  try {
    let dashRes
    if (role === 'STUDENT') {
      dashRes = await getStudentDashboard()
    } else {
      dashRes = await getTeacherDashboard()
    }
    const d = dashRes.data || {}
    todayCourses.value = d.recentActivities || []
    pendingTasks.value = [
      ...(d.pendingRepairs ? [{ title: '待处理报修', count: d.pendingRepairs, link: '/repair' }] : []),
      ...(d.upcomingReservations ? [{ title: '即将到来的预约', count: d.upcomingReservations, link: '/reservation' }] : []),
    ]
  } catch (error) { reportDashboardError('个人仪表盘', error) }

  if (isApprover.value) loadPendingApprovals()

  try {
    const noticeRes = await getNotices({ page: 1, pageSize: 5 })
    recentNotices.value = (noticeRes.data?.records || noticeRes.data || []).map((n: any) => ({
      id: n.id, title: n.title, time: n.createdAt, unread: !n.isRead,
    }))
  } catch (error) { reportDashboardError('近期通知', error) }

  try {
    const actRes = await getActivities({ page: 1, pageSize: 3 })
    recommendedActivities.value = (actRes.data?.records || actRes.data || []).map((a: any) => ({
      id: a.id, title: a.title, date: a.startTime, location: a.location,
      registered: a.registeredCount, capacity: a.capacity,
    }))
  } catch (error) { reportDashboardError('推荐活动', error) }
})

async function loadPendingApprovals() {
  try {
    const res = await getPendingApprovals({ page: 1, pageSize: 6 })
    pendingApprovals.value = (res.data as any)?.records || (res.data as any) || []
  } catch (error) { reportDashboardError('待审批列表', error) }
  await loadPendingCount()
}

async function loadPendingCount() {
  try {
    const res = await getPendingApprovalsCount()
    pendingCount.value = (res.data as any) ?? 0
  } catch (error) { reportDashboardError('待审批数量', error) }
}
</script>
