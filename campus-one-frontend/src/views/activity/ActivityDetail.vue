<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3 mb-4">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="flex-1">
          <h1 class="text-xl font-semibold text-ink-900">{{ activity.title || '加载中...' }}</h1>
          <p class="text-sm text-ink-500 n-rec">{{ activity.activityNo }}</p>
        </div>
      </div>
      <div class="h-48 bg-ink-900/4 border border-line rounded-lg flex items-center justify-center mb-4">
        <el-icon :size="64" class="text-ink-900/20"><Flag /></el-icon>
      </div>
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div><span class="text-ink-500">时间：</span><span class="text-ink-900">{{ activity.startTime }} - {{ activity.endTime }}</span></div>
        <div><span class="text-ink-500">地点：</span><span class="text-ink-900">{{ activity.location }}</span></div>
        <div><span class="text-ink-500">主办方：</span><span class="text-ink-900">{{ activity.organizer }}</span></div>
        <div><span class="text-ink-500">已报名：</span><span class="text-ink-900">{{ activity.enrolledCount }}/{{ activity.maxCount }}人</span></div>
      </div>
      <el-button class="mt-4" type="primary" size="large" :disabled="false" @click="handleRegister">
        立即报名
      </el-button>
    </div>

    <div class="sheet p-6">
      <h2 class="arch-sec mb-3">活动介绍</h2>
      <p class="text-sm text-ink-900 leading-relaxed">
        {{ activity.description }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Flag } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getActivityById, registerActivity } from '@/api/activity'

const route = useRoute()
const activity = ref<any>({})
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getActivityById(id)
    activity.value = res.data || {}
  } catch {} finally { loading.value = false }
})

async function handleRegister() {
  try {
    await registerActivity(activity.value.id)
    ElMessage.success('报名成功')
  } catch {}
}
</script>
