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

    <div v-loading="loading" class="sheet overflow-hidden">
      <div class="px-6 py-4 border-b border-line">
        <el-radio-group v-model="activeType">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="school">学校通知</el-radio-button>
          <el-radio-button value="college">学院通知</el-radio-button>
          <el-radio-button value="class">班级通知</el-radio-button>
          <el-radio-button value="system">系统通知</el-radio-button>
        </el-radio-group>
      </div>

      <el-alert
        v-if="errorMessage"
        class="m-6"
        type="error"
        :title="errorMessage"
        show-icon
        :closable="false"
      >
        <template #default><el-button link type="primary" @click="loadNotices">重新加载</el-button></template>
      </el-alert>

      <div v-else class="divide-y divide-line/70">
        <div
          v-for="(notice, i) in notices"
          :key="notice.id"
          class="px-6 py-4 hover:bg-ink-900/4 transition-colors cursor-pointer flex items-start gap-3"
          @click="$router.push(`/notice/detail/${notice.id}`)"
        >
          <span class="n-rec text-[10px] text-ink-300 mt-1 shrink-0">{{ pad(i + 1) }}</span>
          <div :class="['w-2 h-2 rounded-full mt-2 shrink-0', !isNoticeRead(notice.isRead) ? 'bg-ink-900' : 'bg-ink-300/60']"></div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span v-if="notice.important" class="stamp text-[10px]">重要</span>
              <h3 class="text-sm font-medium text-ink-900 truncate">{{ notice.title }}</h3>
            </div>
            <p class="text-xs text-ink-500 mt-1 line-clamp-1">{{ notice.summary || notice.content }}</p>
            <div class="flex items-center gap-3 mt-2 text-xs text-ink-500">
              <span>{{ notice.source || notice.type || '校园通知' }}</span>
              <span>{{ notice.time || notice.createdAt }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !errorMessage && notices.length === 0" description="暂无校园资讯" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getNotices } from '@/api/notice'
import type { NoticeItem } from '@/api/notice'
import { isNoticeRead } from '@/utils/notice'

const activeType = ref('all')
const notices = ref<NoticeItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

function pad(n: number) {
  return String(n).padStart(2, '0')
}

async function loadNotices() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getNotices({ page: 1, pageSize: 20, type: activeType.value })
    notices.value = res.data?.records || []
  } catch (error) {
    console.error('校园资讯加载失败', error)
    errorMessage.value = '校园资讯加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadNotices)
watch(activeType, loadNotices)
</script>
