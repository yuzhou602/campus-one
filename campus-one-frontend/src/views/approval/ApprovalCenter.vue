<template>
  <div class="space-y-6">
    <div class="sheet p-6">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">审批中心</h1>
          <p class="arch-sub">逐份审阅、记录在案的事由申请。</p>
        </div>
        <span class="arch-mark n-rec">FILE · 三卷</span>
      </div>
    </div>

    <div class="sheet">
      <div class="px-6 py-4 border-b border-line">
        <el-radio-group v-model="activeTab">
          <el-radio-button value="pending">待我审批{{ pendingCount ? ' · ' + pendingCount : '' }}</el-radio-button>
          <el-radio-button value="approved">我已审批</el-radio-button>
          <el-radio-button value="mine">我发起的</el-radio-button>
        </el-radio-group>
      </div>

      <div class="p-6 space-y-4" v-loading="loading">
        <template v-if="activeTab === 'pending'">
          <div
            v-for="item in pendingApprovals"
            :key="item.id"
            class="p-4 border border-line rounded-lg bg-surface hover:border-ink-500 transition-colors"
          >
            <div class="flex items-start justify-between gap-4 flex-wrap">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2.5 flex-wrap">
                  <span class="n-rec text-[10px] text-ink-300">{{ pad(pendingIndex(item)) }}</span>
                  <h3 class="text-sm font-semibold text-ink-900">{{ item.applicationNo || '待审批申请' }}</h3>
                  <span class="stamp text-[10px]">待审</span>
                  <span v-if="overdueDays(item) > 0" class="stamp-line text-[10px]">滞压 {{ overdueDays(item) }} 天</span>
                </div>
                <div class="mt-2 text-sm text-ink-700">
                  <span class="font-medium text-ink-900">{{ item.applicantName || '申请人 #' + item.applicantId }}</span>
                  <span class="text-ink-300 mx-1.5">·</span>
                  {{ item.currentNode || '审批节点' }}
                </div>
              </div>
              <div class="flex items-center gap-2">
                <el-button type="primary" size="small" @click="handleApprove(item.id)">通过</el-button>
                <el-button size="small" @click="handleReject(item.id)">驳回</el-button>
                <el-button size="small" text @click="handleRollback(item.id)">退回上一步</el-button>
                <el-button size="small" text @click="$router.push(`/application/detail/${item.id}`)">详情 →</el-button>
              </div>
            </div>
          </div>
          <el-empty v-if="!pendingApprovals.length" description="待审卷已清空，无在途申请" />
        </template>

        <template v-else-if="activeTab === 'approved'">
          <div
            v-for="(item, i) in processedApprovals"
            :key="item.id"
            class="p-4 border border-line rounded-lg bg-surface hover:border-ink-500 transition-colors"
          >
            <div class="flex items-start justify-between gap-4 flex-wrap">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2.5 flex-wrap">
                  <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
                  <h3 class="text-sm font-semibold text-ink-900">{{ item.applicationNo || '历史申请' }}</h3>
                  <span :class="['stamp', item.status === 'REJECTED' ? 'stamp-line' : 'stamp-line', 'text-[10px]']">
                    {{ item.status === 'REJECTED' ? '已驳回' : '已办结' }}
                  </span>
                </div>
                <div class="mt-2 text-sm text-ink-700">
                  <span class="font-medium text-ink-900">{{ item.applicantName || '申请人 #' + item.applicantId }}</span>
                  <span class="text-ink-300 mx-1.5">·</span>
                  我经手处理
                </div>
              </div>
              <el-button size="small" text @click="$router.push(`/application/detail/${item.id}`)">查看卷宗 →</el-button>
            </div>
          </div>
          <el-empty v-if="!processedApprovals.length" description="还没有经你审批的卷宗" />
        </template>

        <template v-else>
          <div
            v-for="(item, i) in myApprovals"
            :key="item.id"
            class="p-4 border border-line rounded-lg bg-surface hover:border-ink-500 transition-colors"
          >
            <div class="flex items-start justify-between gap-4 flex-wrap">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2.5 flex-wrap">
                  <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
                  <h3 class="text-sm font-semibold text-ink-900">{{ item.applicationNo || '发起的申请' }}</h3>
                  <span :class="['stamp', item.status === 'PENDING' ? '' : 'stamp-line', 'text-[10px]']">
                    {{ statusText(item.status) }}
                  </span>
                </div>
                <div class="mt-2 text-sm text-ink-700">
                  事由 <span class="text-ink-500">·</span> 由我发起
                </div>
              </div>
              <el-button size="small" text @click="$router.push(`/application/detail/${item.id}`)">查看进度 →</el-button>
            </div>
          </div>
          <el-empty v-if="!myApprovals.length" description="尚未发起任何申请" />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getPendingApprovals, getProcessedApprovals, getMyApplications,
  getPendingApprovalsCount, approveTask, rejectTask, rollbackTask,
} from '@/api/application'

const activeTab = ref<'pending' | 'approved' | 'mine'>('pending')
const pendingApprovals = ref<any[]>([])
const processedApprovals = ref<any[]>([])
const myApprovals = ref<any[]>([])
const pendingCount = ref(0)
const loading = ref(false)

const OVERDUE_DAYS = 3

function pad(n: number) {
  return String(n).padStart(2, '0')
}
function pendingIndex(item: any) {
  return pendingApprovals.value.indexOf(item) + 1
}
function statusText(status: string) {
  const m: Record<string, string> = {
    PENDING: '审批中', APPROVED: '已办结', REJECTED: '已驳回',
  }
  return m[status] || status || '未知'
}
/** 超时提醒：处理中的卷宗停留超过阈值天数标记为滞压 */
function overdueDays(item: any): number {
  if (item.status !== 'PENDING') return 0
  const start = item.submittedAt || item.createdAt
  if (!start) return 0
  const days = Math.floor((Date.now() - new Date(start).getTime()) / 86400000)
  return Math.max(days - OVERDUE_DAYS, 0)
}

async function loadCount() {
  try {
    const res = await getPendingApprovalsCount()
    pendingCount.value = (res.data as any) ?? 0
  } catch {}
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingApprovals({ page: 1, pageSize: 20 })
    pendingApprovals.value = (res.data as any)?.records || (res.data as any) || []
  } catch {} finally { loading.value = false }
}

async function loadProcessed() {
  loading.value = true
  try {
    const res = await getProcessedApprovals({ page: 1, pageSize: 20 })
    processedApprovals.value = (res.data as any)?.records || (res.data as any) || []
  } catch {} finally { loading.value = false }
}

async function loadMine() {
  loading.value = true
  try {
    const res = await getMyApplications()
    myApprovals.value = (res.data as any) || []
  } catch {} finally { loading.value = false }
}

watch(activeTab, tab => {
  if (tab === 'pending') loadPending()
  else if (tab === 'approved') loadProcessed()
  else loadMine()
}, { immediate: true })

loadCount()

async function handleApprove(id: number) {
  try {
    await approveTask(id, { action: 'APPROVE', comment: '' })
    ElMessage.success('已通过')
    pendingApprovals.value = pendingApprovals.value.filter(a => a.id !== id)
  } catch {}
}

async function handleReject(id: number) {
  try {
    await rejectTask(id, { action: 'REJECT', comment: '' })
    ElMessage.success('已驳回')
    pendingApprovals.value = pendingApprovals.value.filter(a => a.id !== id)
  } catch {}
}

async function handleRollback(id: number) {
  try {
    await rollbackTask(id, '审批人退回上一步')
    ElMessage.success('已退回上一步')
    pendingApprovals.value = pendingApprovals.value.filter(a => a.id !== id)
  } catch {}
}
</script>