<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">校园资讯</h1>
          <p class="arch-sub">翻阅在卷的校园动态。</p>
        </div>
        <span class="arch-mark n-rec">BULLETIN · 在档 {{ notices.length }} 则</span>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <div class="px-6 py-4 border-b border-line">
        <el-radio-group v-model="activeType">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="school">学校通知</el-radio-button>
          <el-radio-button value="college">学院通知</el-radio-button>
          <el-radio-button value="class">班级通知</el-radio-button>
          <el-radio-button value="system">系统通知</el-radio-button>
        </el-radio-group>
      </div>

      <div class="divide-y divide-line/70">
        <div
          v-for="(notice, i) in notices"
          :key="notice.id"
          class="px-6 py-4 hover:bg-ink-900/4 transition-colors cursor-pointer flex items-start gap-3"
          @click="$router.push(`/notice/detail/${notice.id}`)"
        >
          <span class="n-rec text-[10px] text-ink-300 mt-1 shrink-0">{{ pad(i + 1) }}</span>
          <div :class="['w-2 h-2 rounded-full mt-2 shrink-0', notice.unread ? 'bg-ink-900' : 'bg-ink-300/60']"></div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span v-if="notice.important" class="stamp text-[10px]">重要</span>
              <h3 class="text-sm font-medium text-ink-900 truncate">{{ notice.title }}</h3>
            </div>
            <p class="text-xs text-ink-500 mt-1 line-clamp-1">{{ notice.summary }}</p>
            <div class="flex items-center gap-3 mt-2 text-xs text-ink-500">
              <span>{{ notice.source }}</span>
              <span>{{ notice.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNotices } from '@/api/notice'

const activeType = ref('all')
const notices = ref<any[]>([])
const loading = ref(false)

function pad(n: number) {
  return String(n).padStart(2, '0')
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getNotices({ page: 1, pageSize: 20 })
    notices.value = res.data?.records || res.data || []
  } catch {} finally {
    loading.value = false
  }
})
</script>
