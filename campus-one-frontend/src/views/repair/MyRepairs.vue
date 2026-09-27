<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">我的报修</h1>
          <p class="arch-sub">已登记的报修工单在案记录。</p>
        </div>
        <span class="arch-mark n-rec">REPAIR · 在档 {{ repairs.length }}</span>
      </div>
    </div>
    <div class="sheet overflow-hidden">
      <el-table :data="repairs" v-loading="loading" empty-text="暂无报修记录">
        <el-table-column prop="repairNo" label="工单号" width="180" />
        <el-table-column prop="location" label="位置" min-width="150" />
        <el-table-column prop="category" label="类型" width="120" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="repair" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/repair/detail/${row.id}`)">查看详情</el-button>
            <el-button v-if="row.status === 'RESOLVED'" type="success" link :loading="updatingId === row.id" @click="confirmResolved(row)">确认完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/common/StatusTag.vue'
import { getMyRepairs, updateRepairStatus } from '@/api/repair'

const loading = ref(false)
const repairs = ref<any[]>([])
const updatingId = ref<number | null>(null)

async function loadRepairs() {
  loading.value = true
  try {
    const res = await getMyRepairs({ page: 1, pageSize: 20 })
    repairs.value = res.data?.records || res.data || []
  } catch (error) {
    repairs.value = []
    console.error('加载报修记录失败', error)
  } finally { loading.value = false }
}

async function confirmResolved(row: any) {
  const confirmed = await ElMessageBox.confirm('确认故障已经解决吗？确认后工单将进入待关闭状态。', '确认维修结果', { type: 'warning' })
    .then(() => true).catch(() => false)
  if (!confirmed) return
  updatingId.value = row.id
  try {
    await updateRepairStatus(row.id, 'CONFIRMED')
    ElMessage.success('已确认维修完成')
    await loadRepairs()
  } catch (error: any) {
    ElMessage.error(error.message || '确认失败')
  } finally {
    updatingId.value = null
  }
}

onMounted(loadRepairs)
</script>
