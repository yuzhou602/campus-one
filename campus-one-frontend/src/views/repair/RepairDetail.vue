<template>
  <div class="max-w-4xl mx-auto space-y-6" v-loading="loading">
    <div class="sheet p-6">
      <div class="flex flex-wrap items-center gap-3">
        <el-button text aria-label="返回" @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="min-w-0 flex-1">
          <h1 class="text-xl font-semibold text-ink-900">{{ repairTitle }}</h1>
          <p class="text-sm text-ink-500 n-rec">{{ repair.repairNo || '工单加载中' }}</p>
        </div>
        <StatusTag v-if="repair.status" :status="repair.status" type="repair" size="large" />
        <el-button v-if="availableAction" type="primary" :loading="updating" @click="advanceStatus">{{ availableAction.label }}</el-button>
      </div>
    </div>

    <div v-if="loadError" class="sheet empty-state py-14">
      <strong>无法加载工单</strong><span>{{ loadError }}</span>
      <el-button class="mt-4" @click="loadRepair">重新加载</el-button>
    </div>

    <template v-else-if="repair.id">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 sheet p-6">
          <h2 class="arch-sec mb-4">故障信息</h2>
          <dl class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4 text-sm">
            <div class="sm:col-span-2"><dt class="text-ink-500">故障描述</dt><dd class="text-ink-900 mt-1 whitespace-pre-wrap">{{ repair.description || '未填写' }}</dd></div>
            <div><dt class="text-ink-500">报修位置</dt><dd class="text-ink-900 mt-1">{{ repair.location || '未填写' }}</dd></div>
            <div><dt class="text-ink-500">故障类型</dt><dd class="text-ink-900 mt-1">{{ repair.category || '未分类' }}</dd></div>
            <div><dt class="text-ink-500">联系方式</dt><dd class="text-ink-900 mt-1">{{ repair.contact || '未填写' }}</dd></div>
            <div><dt class="text-ink-500">可维修时间</dt><dd class="text-ink-900 mt-1">{{ repair.availableTime || '未填写' }}</dd></div>
            <div><dt class="text-ink-500">优先级</dt><dd class="text-ink-900 mt-1">{{ repair.priority || 'MEDIUM' }}</dd></div>
            <div><dt class="text-ink-500">提交时间</dt><dd class="text-ink-900 mt-1">{{ formatTime(repair.createdAt) }}</dd></div>
          </dl>
        </div>
        <div class="sheet p-6">
          <h2 class="arch-sec mb-4">处理归属</h2>
          <div v-if="repair.assignedUserId" class="space-y-2 text-sm">
            <div class="w-12 h-12 rounded-full bg-ink-900 text-white flex items-center justify-center text-lg">修</div>
            <p class="font-medium text-ink-900">维修人员 #{{ repair.assignedUserId }}</p>
            <p class="text-xs text-ink-500">工单仅允许该人员和管理员处理</p>
          </div>
          <div v-else class="text-sm text-ink-500">尚未分派维修人员</div>
        </div>
      </div>

      <div class="sheet p-6">
        <h2 class="arch-sec mb-4">工单时间线</h2>
        <el-timeline>
          <el-timeline-item v-for="item in timeline" :key="item.label" :timestamp="item.time" :type="item.type">{{ item.label }}</el-timeline-item>
        </el-timeline>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/common/StatusTag.vue'
import { acceptRepair, getRepairById, updateRepairStatus } from '@/api/repair'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const repair = ref<any>({})
const loading = ref(false)
const updating = ref(false)
const loadError = ref('')

const repairTitle = computed(() => repair.value.title || repair.value.description?.slice(0, 24) || '报修详情')
const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userStore.role))
const isOwner = computed(() => repair.value.userId === userStore.userInfo?.id)
const isAssignee = computed(() => repair.value.assignedUserId === userStore.userInfo?.id)

const availableAction = computed<{ label: string; status: string; accept?: boolean } | null>(() => {
  const status = repair.value.status
  if (status === 'RESOLVED' && (isOwner.value || isAdmin.value)) return { label: '确认维修完成', status: 'CONFIRMED' }
  if (status === 'CONFIRMED' && isAdmin.value) return { label: '关闭工单', status: 'CLOSED' }
  if (!(isAssignee.value || isAdmin.value)) return null
  if (status === 'ASSIGNED') return { label: '接单', status: 'ACCEPTED', accept: true }
  if (status === 'ACCEPTED') return { label: '开始处理', status: 'PROCESSING' }
  if (status === 'PROCESSING') return { label: '标记已解决', status: 'RESOLVED' }
  return null
})

const timeline = computed(() => {
  const items: Array<{ label: string; time: string; type: any }> = [
    { label: '用户提交报修工单', time: formatTime(repair.value.createdAt), type: 'primary' },
  ]
  if (repair.value.assignedUserId) items.push({ label: `工单已分派给维修人员 #${repair.value.assignedUserId}`, time: '', type: 'info' })
  if (repair.value.acceptedAt) items.push({ label: '维修人员已接单', time: formatTime(repair.value.acceptedAt), type: 'primary' })
  if (['PROCESSING', 'RESOLVED', 'CONFIRMED', 'CLOSED'].includes(repair.value.status)) items.push({ label: '维修处理中', time: '', type: 'warning' })
  if (repair.value.resolvedAt) items.push({ label: '维修人员标记问题已解决', time: formatTime(repair.value.resolvedAt), type: 'success' })
  if (['CONFIRMED', 'CLOSED'].includes(repair.value.status)) items.push({ label: '报修人已确认维修结果', time: '', type: 'success' })
  if (repair.value.status === 'CLOSED') items.push({ label: '工单已关闭', time: formatTime(repair.value.closedAt), type: 'info' })
  return items
})

function formatTime(value?: string) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }

async function loadRepair() {
  const id = Number(route.params.id)
  if (!id) { loadError.value = '工单编号无效'; return }
  loading.value = true
  loadError.value = ''
  try {
    const response = await getRepairById(id)
    repair.value = response.data || {}
  } catch (error: any) {
    repair.value = {}
    loadError.value = error.message || '加载工单失败'
  } finally { loading.value = false }
}

async function advanceStatus() {
  const action = availableAction.value
  if (!action) return
  const confirmed = await ElMessageBox.confirm(`确定执行“${action.label}”吗？`, '更新工单', { type: 'warning' }).then(() => true).catch(() => false)
  if (!confirmed) return
  updating.value = true
  try {
    if (action.accept) await acceptRepair(repair.value.id)
    else await updateRepairStatus(repair.value.id, action.status)
    ElMessage.success('工单状态已更新')
    await loadRepair()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally { updating.value = false }
}

onMounted(loadRepair)
</script>
