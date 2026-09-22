<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">任务中心</h1>
          <p class="arch-sub">统一管理待办的登记事项。</p>
        </div>
        <span class="arch-mark n-rec">TASKS · 统一在案</span>
      </div>
    </div>

    <div v-if="loading" class="grid grid-cols-1 gap-4 md:grid-cols-3" aria-live="polite">
      <div v-for="n in 3" :key="n" class="sheet p-5"><el-skeleton :rows="4" animated /></div>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="sheet p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="arch-sec">待审批</span>
          <span class="stamp stamp-line text-[10px]">{{ pendingApprovals.length }}</span>
        </div>
        <div class="space-y-2">
          <button v-for="item in pendingApprovals" :key="item.id" type="button" class="task-entry" @click="router.push(`/application/detail/${item.id}`)">
            <span>{{ item.applicationNo || '待审批申请' }}</span><span aria-hidden="true">→</span>
          </button>
          <p v-if="!pendingApprovals.length" class="empty-copy">暂无待审批事项</p>
        </div>
      </div>

      <div class="sheet p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="arch-sec">我的预约</span>
          <span class="stamp stamp-line text-[10px]">{{ myReservations.length }}</span>
        </div>
        <div class="space-y-2">
          <button v-for="item in myReservations" :key="item.id" type="button" class="task-entry" @click="router.push('/reservation')">
            <span>{{ item.reservationNo || item.resourceName || '场地预约' }}</span><span class="n-rec text-[10px]">{{ item.reservationDate || '' }}</span>
          </button>
          <p v-if="!myReservations.length" class="empty-copy">暂无即将到来的预约</p>
        </div>
      </div>

      <div class="sheet p-5">
        <div class="flex items-center justify-between mb-3">
          <span class="arch-sec">我的报修</span>
          <span class="stamp stamp-line text-[10px]">{{ myRepairs.length }}</span>
        </div>
        <div class="space-y-2">
          <button v-for="item in myRepairs" :key="item.id" type="button" class="task-entry" @click="router.push(`/repair/detail/${item.id}`)">
            <span>{{ item.repairNo || '报修工单' }}</span><span aria-hidden="true">→</span>
          </button>
          <p v-if="!myRepairs.length" class="empty-copy">暂无进行中的报修</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyTasks } from '@/api/task'

const router = useRouter()

const pendingApprovals = ref<any[]>([])
const myReservations = ref<any[]>([])
const myRepairs = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyTasks()
    const data = res.data || {}
    pendingApprovals.value = data.pendingApprovals?.records || data.pendingApprovals || []
    myReservations.value = data.myReservations?.records || data.myReservations || []
    myRepairs.value = data.myRepairs?.records || data.myRepairs || []
  } catch (error) {
    console.error('加载任务中心失败', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.task-entry {
  display: flex;
  min-height: 42px;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 7px;
  background: rgb(34 29 24 / 0.04);
  padding: 9px 10px;
  color: var(--color-ink-700);
  font-size: 12px;
  text-align: left;
  transition: background-color 150ms ease, color 150ms ease;
}
.task-entry:hover { background: rgb(34 29 24 / 0.08); color: var(--color-ink-900); }
.empty-copy { padding: 18px 8px; text-align: center; font-size: 12px; color: var(--color-ink-300); }
</style>
