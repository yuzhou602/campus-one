<template>
  <div class="space-y-6">
    <div class="sheet p-6">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">校园办事大厅</h1>
          <p class="arch-sub">为每一件校园小事，登记一张凭证。</p>
        </div>
        <span class="arch-mark n-rec">SVC · 目录 {{ services.length }} 项</span>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="(service, i) in services"
        :key="service.id"
        class="sheet p-5 cursor-pointer group/item hover:border-ink-500 transition-colors"
        @click="$router.push(`/service/apply/${service.id}`)"
      >
        <div class="flex items-start gap-4">
          <div class="w-11 h-11 border-2 border-ink-900/15 rounded-lg flex items-center justify-center shrink-0 group-hover/item:border-ink-900/50 transition-colors bg-surface">
            <el-icon :size="20" class="text-ink-700 group-hover/item:text-ink-900"><component :is="service.icon" /></el-icon>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
              <h3 class="text-sm font-semibold text-ink-900">{{ service.name }}</h3>
            </div>
            <p class="text-xs text-ink-500 mt-1.5 leading-relaxed">{{ service.description }}</p>
          </div>
        </div>
        <div class="mt-4 pt-3 border-t border-line/70">
          <div class="flex items-center justify-between text-xs text-ink-500">
            <span>适用 · {{ service.audience }}</span>
            <span class="n-rec">约 {{ service.duration }}</span>
          </div>
          <div class="text-xs text-ink-500 mt-1">审批 · {{ service.approvalFlow }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getServices } from '@/api/application'

const services = ref<any[]>([])
const loading = ref(false)

function pad(n: number) {
  return String(n).padStart(2, '0')
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getServices()
    services.value = res.data || []
  } catch {} finally { loading.value = false }
})
</script>