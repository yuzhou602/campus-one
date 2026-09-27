<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">我的预约</h1>
          <p class="arch-sub">查看预约状态，并取消尚未开始的预约。</p>
        </div>
        <el-button type="primary" @click="$router.push('/reservation')">预约场地</el-button>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <el-table :data="reservations" v-loading="loading" empty-text="暂无预约记录">
        <el-table-column prop="reservationNo" label="预约编号" min-width="170" />
        <el-table-column prop="resourceName" label="场地" min-width="160">
          <template #default="{ row }">{{ row.resourceName || `场地 #${row.resourceId}` }}</template>
        </el-table-column>
        <el-table-column prop="reservationDate" label="日期" width="120" />
        <el-table-column label="时段" width="140">
          <template #default="{ row }">{{ row.startTime }}–{{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" type="reservation" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/reservation/detail/${row.resourceId}`)">查看场地</el-button>
            <el-button v-if="canCancel(row)" type="danger" link :loading="cancellingId === row.id" @click="handleCancel(row)">取消</el-button>
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
import StatusTag from '@/components/common/StatusTag.vue'
import { cancelReservation, getReservations } from '@/api/reservation'
import type { ResourceReservation } from '@/types/reservation'

const loading = ref(false)
const cancellingId = ref<number | null>(null)
const reservations = ref<ResourceReservation[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)

function canCancel(item: any) {
  return item.status === 'PENDING' || item.status === 'CONFIRMED'
}

async function loadReservations() {
  loading.value = true
  try {
    const response = await getReservations({ page: page.value, pageSize })
    reservations.value = response.data?.records || []
    total.value = Number(response.data?.total || reservations.value.length)
  } catch (error) {
    reservations.value = []
    total.value = 0
    console.error('加载预约记录失败', error)
  } finally {
    loading.value = false
  }
}

async function handleCancel(item: any) {
  const confirmed = await ElMessageBox.confirm('确定取消这条预约吗？', '取消预约', { type: 'warning' })
    .then(() => true).catch(() => false)
  if (!confirmed) return
  cancellingId.value = item.id
  try {
    await cancelReservation(item.id)
    ElMessage.success('预约已取消')
    await loadReservations()
  } catch (error: any) {
    ElMessage.error(error.message || '取消失败')
  } finally {
    cancellingId.value = null
  }
}

onMounted(loadReservations)
</script>
