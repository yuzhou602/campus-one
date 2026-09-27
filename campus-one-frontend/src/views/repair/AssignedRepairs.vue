<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">{{ isAdmin ? '工单分派' : '维修工作台' }}</h1>
          <p class="arch-sub">{{ isAdmin ? '将新报修分派给启用的维修服务人员。' : '只展示明确分派给当前账号的维修工单。' }}</p>
        </div>
        <span class="arch-mark n-rec">WORK · {{ total }} 件</span>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <el-table :data="repairs" v-loading="loading" empty-text="暂无已分派工单">
        <el-table-column prop="repairNo" label="工单号" min-width="170" />
        <el-table-column prop="location" label="位置" min-width="170" />
        <el-table-column prop="category" label="类型" width="120" />
        <el-table-column prop="description" label="故障描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" type="repair" /></template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="维修人员" min-width="180">
          <template #default="{ row }">
            <el-select v-model="selectedTechnicians[row.id]" placeholder="选择维修人员" filterable>
              <el-option v-for="item in technicians" :key="item.id" :label="item.realName || item.username" :value="item.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/repair/detail/${row.id}`)">详情</el-button>
            <el-button v-if="isAdmin" type="success" link :disabled="!selectedTechnicians[row.id]" :loading="updatingId === row.id" @click="dispatch(row)">确认分派</el-button>
            <el-button v-else-if="nextAction(row)" type="success" link :loading="updatingId === row.id" @click="advance(row)">{{ nextAction(row)?.label }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="flex justify-end p-4 border-t border-line">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadRepairs" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/common/StatusTag.vue'
import { acceptRepair, assignRepair, getAssignedRepairs, getRepairTechnicians, getUnassignedRepairs, updateRepairStatus } from '@/api/repair'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userStore.role))
const repairs = ref<any[]>([])
const technicians = ref<Array<{ id: number; username: string; realName: string }>>([])
const selectedTechnicians = reactive<Record<number, number | undefined>>({})
const loading = ref(false)
const updatingId = ref<number | null>(null)
const page = ref(1)
const pageSize = 20
const total = ref(0)

function nextAction(row: any) {
  const actions: Record<string, { label: string; status?: string }> = {
    ASSIGNED: { label: '接单' },
    ACCEPTED: { label: '开始处理', status: 'PROCESSING' },
    PROCESSING: { label: '标记已解决', status: 'RESOLVED' },
  }
  return actions[row.status]
}

async function loadRepairs() {
  loading.value = true
  try {
    const response = isAdmin.value
      ? await getUnassignedRepairs({ page: page.value, pageSize })
      : await getAssignedRepairs({ page: page.value, pageSize })
    repairs.value = response.data?.records || []
    total.value = Number(response.data?.total || repairs.value.length)
  } catch (error) {
    repairs.value = []
    total.value = 0
    console.error('加载维修任务失败', error)
  } finally {
    loading.value = false
  }
}

async function dispatch(row: any) {
  const technicianId = selectedTechnicians[row.id]
  if (!technicianId) return
  const technician = technicians.value.find(item => item.id === technicianId)
  const confirmed = await ElMessageBox.confirm(`确定将工单分派给“${technician?.realName || technician?.username}”吗？`, '分派工单', { type: 'warning' })
    .then(() => true).catch(() => false)
  if (!confirmed) return
  updatingId.value = row.id
  try {
    await assignRepair(row.id, technicianId)
    ElMessage.success('工单已分派')
    delete selectedTechnicians[row.id]
    await loadRepairs()
  } catch (error: any) {
    ElMessage.error(error.message || '分派失败')
  } finally {
    updatingId.value = null
  }
}

async function advance(row: any) {
  const action = nextAction(row)
  if (!action) return
  const confirmed = await ElMessageBox.confirm(`确定执行“${action.label}”吗？`, '更新工单', { type: 'warning' })
    .then(() => true).catch(() => false)
  if (!confirmed) return
  updatingId.value = row.id
  try {
    if (row.status === 'ASSIGNED') await acceptRepair(row.id)
    else await updateRepairStatus(row.id, action.status!)
    ElMessage.success('工单状态已更新')
    await loadRepairs()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    updatingId.value = null
  }
}

onMounted(async () => {
  if (isAdmin.value) {
    try {
      const response = await getRepairTechnicians()
      technicians.value = response.data || []
    } catch (error) {
      console.error('加载维修人员失败', error)
    }
  }
  await loadRepairs()
})
</script>
