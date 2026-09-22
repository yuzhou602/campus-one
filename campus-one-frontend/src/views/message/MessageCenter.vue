<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">消息中心</h1>
          <p class="arch-sub">查阅各类来函与事项通知。</p>
        </div>
        <span class="arch-mark n-rec">MAIL · 收件 {{ messages.length }} 条</span>
      </div>
    </div>

    <div class="sheet overflow-hidden">
      <div class="px-6 py-4 border-b border-line flex items-center justify-between">
        <el-radio-group v-model="activeType">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="approval">审批</el-radio-button>
          <el-radio-button value="reservation">预约</el-radio-button>
          <el-radio-button value="repair">报修</el-radio-button>
          <el-radio-button value="activity">活动</el-radio-button>
          <el-radio-button value="system">系统</el-radio-button>
        </el-radio-group>
        <el-button text type="primary" @click="handleMarkAllRead">全部已读</el-button>
      </div>

      <div class="divide-y divide-line/70">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['px-6 py-4 hover:bg-ink-900/4 transition-colors cursor-pointer flex items-start gap-3', !msg.read ? 'bg-ink-900/4' : '']"
          @click="handleClick(msg)"
        >
          <div class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 bg-ink-900/5 border border-line">
            <el-icon :size="18" class="text-ink-700"><component :is="msg.icon" /></el-icon>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-medium text-ink-900 truncate">{{ msg.title }}</h3>
              <span class="text-xs text-ink-500 shrink-0 ml-2">{{ msg.time }}</span>
            </div>
            <p class="text-xs text-ink-500 mt-1 line-clamp-1">{{ msg.content }}</p>
          </div>
          <div v-if="!msg.read" class="w-2 h-2 rounded-full bg-ink-900 shrink-0 mt-2"></div>
        </div>
      </div>

      <el-empty v-if="messages.length === 0" description="暂无消息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMessages, markAllAsRead } from '@/api/message'

const activeType = ref('all')
const messages = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMessages({ page: 1, pageSize: 20 })
    messages.value = res.data || []
  } catch {} finally { loading.value = false }
})

async function handleClick(msg: any) {
  msg.read = true
  if (msg.link) {
    // router.push(msg.link)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    messages.value.forEach(m => m.isRead = true)
  } catch {}
}
</script>