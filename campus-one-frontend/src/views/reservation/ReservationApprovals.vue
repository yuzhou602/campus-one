<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">预约审核</h1>
          <p class="arch-sub">审核标记为“需审批”的场地预约，并记录处理意见。</p>
        </div>
        <span class="arch-mark n-rec">PENDING · {{ total }} 件</span>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <el-table :data="reservations" v-loading="loading" empty-text="暂无待审核预约">
        <el-table-column prop="reservationNo" label="预约编号" min-width="170" />
        <el-table-column label="场地" min-width="150">
          <template #default="{ row }">{{ row.resourceName || `场地 #${row.resourceId}` }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="120">
          <template #default="{ row }">{{ row.userName || `用户 #${row.userId}` }}</template>
        </el-table-column>
        <el-table-column prop="reservationDate" label="日期" width="120" />
        <el-table-column label="时段" width="140"><template #default="{ row }">{{ row.startTime }}–{{ row.endTime }}</template></el-table-column>
        <el-table-column prop="participantCount" label="人数" width="80" />
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link :loading="processingId === row.id" @click="approve(row)">通过</el-button>
            <el-button type="danger" link :loading="processingId === row.id" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="flex justify-end p-4 border-t border-line">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadReservations" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveReservation, getPendingReservations, rejectReservation } from '@/api/reservation'
import type { ResourceReservation } from '@/types/reservation'

const reservations = ref<ResourceReservation[]>([])
const loading = ref(false)
const processingId = ref<number | null>(null)
const page = ref(1)
const pageSize = 20
const total = ref(0)

async function loadReservations() {
  loading.value = true
  try {
    const response = await getPendingReservations({ page: page.value, pageSize })
    reservations.value = response.data?.records || []
    total.value = Number(response.data?.total || reservations.value.length)
  } catch (error) {
    reservations.value = []
    total.value = 0
    console.error('加载待审核预约失败', error)
  } finally { loading.value = false }
}

async function approve(row: any) {
  const confirmed = await ElMessageBox.confirm('确认通过这条预约吗？', '通过预约', { type: 'warning' }).then(() => true).catch(() => false)
  if (!confirmed) return
  processingId.value = row.id
  try {
    await approveReservation(row.id, '审核通过')
    ElMessage.success('预约已通过')
    await loadReservations()
  } catch (error: any) {
    ElMessage.error(error.message || '审核失败')
  } finally { processingId.value = null }
}

async function reject(row: any) {
  const result = await ElMessageBox.prompt('请输入驳回原因', '驳回预约', {
    inputType: 'textarea', inputPlaceholder: '请说明无法通过的原因',
    inputValidator: value => Boolean(value?.trim()) || '驳回原因不能为空',
  }).catch(() => null)
  if (!result) return
  processingId.value = row.id
  try {
    await rejectReservation(row.id, result.value.trim())
    ElMessage.success('预约已驳回')
    await loadReservations()
  } catch (error: any) {
    ElMessage.error(error.message || '审核失败')
  } finally { processingId.value = null }
}

onMounted(loadReservations)
</script>
