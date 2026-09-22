<template>
  <el-tag :type="tagType" :size="size" effect="light" round>
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  type: 'approval' | 'repair' | 'reservation'
  size?: 'large' | 'default' | 'small'
}>()

const approvalMap: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: '' },
  PROCESSING: { label: '审批中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  WITHDRAWN: { label: '已撤回', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

const repairMap: Record<string, { label: string; type: string }> = {
  SUBMITTED: { label: '已提交', type: '' },
  ASSIGNED: { label: '已分配', type: 'primary' },
  ACCEPTED: { label: '已接单', type: 'primary' },
  PROCESSING: { label: '处理中', type: 'warning' },
  WAITING_PART: { label: '待配件', type: 'warning' },
  RESOLVED: { label: '已解决', type: 'success' },
  CONFIRMED: { label: '已确认', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

const reservationMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待确认', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  IN_USE: { label: '使用中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
  EXPIRED: { label: '已过期', type: 'info' },
  REJECTED: { label: '已拒绝', type: 'danger' },
}

const map = computed(() => {
  if (props.type === 'approval') return approvalMap
  if (props.type === 'repair') return repairMap
  return reservationMap
})

const statusInfo = computed(() => map.value[props.status] || { label: props.status, type: 'info' })
const label = computed(() => statusInfo.value.label)
const tagType = computed(() => statusInfo.value.type as any)
</script>
