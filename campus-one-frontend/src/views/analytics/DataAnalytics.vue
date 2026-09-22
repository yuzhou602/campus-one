<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">数据中心</h1>
          <p class="arch-sub">校园运营数据——在案实时，墨笔为准。</p>
        </div>
        <span class="arch-mark n-rec">DATA · 实时在案</span>
      </div>
    </div>

    <!-- 总览统计卡 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="(stat, i) in stats" :key="stat.label" class="sheet p-5">
        <div class="flex items-center justify-between">
          <span class="text-xs text-ink-500">{{ stat.label }}</span>
          <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
        </div>
        <div class="text-3xl font-semibold text-ink-900 mt-2 tabular-nums">{{ stat.value }}</div>
        <div class="text-xs mt-1 text-ink-300">{{ stat.hint }}</div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 场地运营 -->
      <div class="sheet p-6">
        <div class="arch-sec mb-3">场地预约 · 运营</div>
        <div class="space-y-5 py-5" aria-label="场地预约统计">
          <div v-for="metric in venueMetrics" :key="metric.label">
            <div class="mb-2 flex items-end justify-between gap-4">
              <span class="text-sm text-ink-500">{{ metric.label }}</span>
              <strong class="n-rec text-lg text-ink-900">{{ metric.value }}</strong>
            </div>
            <div class="h-2.5 overflow-hidden rounded-full bg-line/60">
              <div class="h-full rounded-full bg-ink-900 transition-[width]" :style="{ width: barWidth(metric.value) }" />
            </div>
          </div>
        </div>
        <p class="text-[11px] text-ink-300 mt-2 n-rec">口径：全部预约 / 今日 / 近30日日均</p>
      </div>

      <!-- 报修分布 -->
      <div class="sheet p-6">
        <div class="arch-sec mb-3">报修工单 · 状态分布</div>
        <div class="py-5" aria-label="报修工单状态分布">
          <div class="flex h-5 overflow-hidden rounded-md bg-line/60" role="img" :aria-label="repairSummary">
            <div
              v-for="(item, index) in repairMetrics"
              :key="item.label"
              :class="['h-full', index === 0 ? 'bg-ink-900' : index === 1 ? 'bg-ink-500' : 'bg-ink-300']"
              :style="{ width: percentage(item.value) }"
            />
          </div>
          <dl class="mt-6 grid grid-cols-3 gap-3">
            <div v-for="(item, index) in repairMetrics" :key="item.label" class="border-l-2 pl-3" :class="index === 0 ? 'border-ink-900' : index === 1 ? 'border-ink-500' : 'border-ink-300'">
              <dt class="text-xs text-ink-500">{{ item.label }}</dt>
              <dd class="n-rec mt-1 text-xl font-semibold text-ink-900">{{ item.value }}</dd>
              <span class="text-[10px] text-ink-300">{{ percentage(item.value) }}</span>
            </div>
          </dl>
        </div>
        <p class="text-[11px] text-ink-300 mt-2 n-rec">口径：已受理 / 处理中 / 已办结</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import {
  getAnalyticsOverview, getVenueAnalytics, getRepairAnalytics,
} from '@/api/dashboard'

const stats = ref<{ label: string; value: string | number; hint: string }[]>([])

const venueData = ref({ totalReservations: 0, todayReservations: 0, avgDaily: 0 })
const repairData = ref({ accepted: 0, submitted: 0, resolved: 0 })

function pad(n: number) {
  return String(n).padStart(2, '0')
}

const venueMetrics = computed(() => [
  { label: '全部预约', value: venueData.value.totalReservations },
  { label: '今日预约', value: venueData.value.todayReservations },
  { label: '近 30 日日均', value: venueData.value.avgDaily },
])
const maxVenueValue = computed(() => Math.max(...venueMetrics.value.map(item => item.value), 1))
const repairMetrics = computed(() => [
  { label: '已受理', value: repairData.value.accepted },
  { label: '处理中', value: repairData.value.submitted },
  { label: '已办结', value: repairData.value.resolved },
])
const repairTotal = computed(() => repairMetrics.value.reduce((sum, item) => sum + item.value, 0))
const repairSummary = computed(() => repairMetrics.value.map(item => `${item.label} ${item.value} 单`).join('，'))

function barWidth(value: number) {
  if (value <= 0) return '0%'
  return `${Math.max((value / maxVenueValue.value) * 100, 3)}%`
}

function percentage(value: number) {
  return repairTotal.value ? `${Math.round((value / repairTotal.value) * 100)}%` : '0%'
}

onMounted(async () => {
  try {
    const res = await getAnalyticsOverview()
    const o = res.data || {}
    stats.value = [
      { label: '在案用户', value: o.totalUsers ?? 0, hint: '平台用户总数' },
      { label: '今日预约', value: o.todayReservations ?? 0, hint: '场地面今日排期' },
      { label: '待处理工单', value: o.pendingRepairs ?? 0, hint: '报修中待受理' },
      { label: '活动在档', value: o.totalActivities ?? 0, hint: '校园活动登记数' },
    ]
  } catch (error) {
    console.error('加载数据总览失败', error)
  }

  try {
    const v = await getVenueAnalytics()
    venueData.value = v.data || venueData.value
  } catch (error) {
    console.error('加载场地运营数据失败', error)
  }

  try {
    const r = await getRepairAnalytics()
    repairData.value = r.data || repairData.value
  } catch (error) {
    console.error('加载报修统计失败', error)
  }
})
</script>
