<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="flex-1">
          <h1 class="text-xl font-semibold text-ink-900">{{ repair.title || '加载中...' }}</h1>
          <p class="text-sm text-ink-500 n-rec">{{ repair.repairNo }}</p>
        </div>
        <StatusTag :status="repair.status" type="repair" size="large" />
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 sheet p-6">
        <h2 class="arch-sec mb-4">故障信息</h2>
        <div class="space-y-3 text-sm">
          <div><span class="text-ink-500">故障描述：</span><span class="text-ink-900">{{ repair.description }}</span></div>
          <div><span class="text-ink-500">报修位置：</span><span class="text-ink-900">{{ repair.location }}</span></div>
          <div><span class="text-ink-500">联系人：</span><span class="text-ink-900">{{ repair.contactName }} {{ repair.contactPhone }}</span></div>
          <div><span class="text-ink-500">提交时间：</span><span class="text-ink-900">{{ repair.createdAt }}</span></div>
          <div><span class="text-ink-500">AI分类：</span><span class="text-ink-900">{{ repair.category }} · {{ repair.urgency }}</span></div>
        </div>
      </div>

      <div class="sheet p-6">
        <h2 class="arch-sec mb-4">维修人员</h2>
        <div class="text-center">
          <el-avatar :size="64" class="bg-ink-900 text-white text-lg">李</el-avatar>
          <div class="text-sm font-medium text-ink-900 mt-3">李师傅</div>
          <div class="text-xs text-ink-500 mt-1">暖通维修组</div>
        </div>
      </div>
    </div>

    <!-- Timeline -->
    <div class="sheet p-6">
      <h2 class="arch-sec mb-4">工单时间线</h2>
      <el-timeline>
        <el-timeline-item timestamp="09:20" type="primary">用户提交报修工单</el-timeline-item>
        <el-timeline-item timestamp="09:23" type="success">AI分类为空调故障</el-timeline-item>
        <el-timeline-item timestamp="09:25" type="info">系统分配暖通维修组</el-timeline-item>
        <el-timeline-item timestamp="09:37" type="success">李师傅接单</el-timeline-item>
        <el-timeline-item timestamp="10:02" type="primary">到达现场</el-timeline-item>
        <el-timeline-item timestamp="10:15" type="warning">处理中</el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getRepairById } from '@/api/repair'

const route = useRoute()
const repair = ref<any>({})
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getRepairById(id)
    repair.value = res.data || {}
  } catch {} finally { loading.value = false }
})
</script>
