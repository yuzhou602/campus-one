<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3 mb-4">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="flex-1">
          <h1 class="text-xl font-semibold text-ink-900">{{ notice.title || '通知详情' }}</h1>
          <div class="flex items-center gap-3 mt-2 text-xs text-ink-500">
            <span>{{ notice.publisher || '' }}</span>
            <span>{{ notice.publishTime || '' }}</span>
            <span v-if="notice.important" class="stamp text-[10px]">重要</span>
          </div>
        </div>
      </div>
    </div>

    <div class="sheet p-6">
      <div class="prose prose-sm max-w-none text-ink-900 whitespace-pre-wrap break-words">{{ notice.content || '暂无正文' }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getNoticeById } from '@/api/notice'

const route = useRoute()
const notice = ref<any>({})
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getNoticeById(id)
    notice.value = res.data || {}
  } catch (error) {
    console.error('通知加载失败', error)
  } finally { loading.value = false }
})
</script>
