<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">场地预约</h1>
          <p class="arch-sub">登记并预约你的校园空间。</p>
        </div>
        <span class="arch-mark n-rec">SPACE · 在册 {{ resources.length }} 处</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="sheet p-6">
      <div class="arch-sec mb-4">检索条件</div>
      <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-3">
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" aria-label="预约日期" class="w-full" size="default" />
        <el-select v-model="filters.resourceType" placeholder="场地类型" clearable class="w-full">
          <el-option label="教室" value="classroom" />
          <el-option label="自习室" value="study" />
          <el-option label="实验室" value="lab" />
          <el-option label="会议室" value="meeting" />
          <el-option label="篮球场" value="basketball" />
          <el-option label="羽毛球馆" value="badminton" />
        </el-select>
        <el-select v-model="filters.buildingId" placeholder="教学楼" clearable class="w-full">
          <el-option label="信息楼" :value="1" />
          <el-option label="教学楼A" :value="2" />
          <el-option label="实验楼" :value="3" />
        </el-select>
        <el-select v-model="filters.capacity" placeholder="容纳人数" clearable class="w-full">
          <el-option label="30人以下" value="small" />
          <el-option label="30-60人" value="medium" />
          <el-option label="60人以上" value="large" />
        </el-select>
        <el-button type="primary" @click="fetchResources">
          <el-icon><Search /></el-icon>检索
        </el-button>
      </div>
    </div>

    <!-- Resource Cards -->
    <div v-if="loading" class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3" aria-live="polite">
      <div v-for="n in 6" :key="n" class="sheet p-5"><el-skeleton :rows="5" animated /></div>
    </div>
    <div v-else-if="resources.length" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="(room, i) in resources"
        :key="room.id"
        class="sheet cursor-pointer hover:border-ink-500 transition-colors overflow-hidden group/item"
      >
        <div class="px-5 pt-5 flex items-start justify-between">
          <span class="arch-sec text-[10px]">场地档案</span>
          <span v-if="room.needApproval" class="stamp text-[10px]">需审批</span>
        </div>
        <div class="p-5 pt-4">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <h3 class="text-base font-semibold text-ink-900 truncate">{{ room.resourceName }}</h3>
              <p class="text-xs text-ink-500 mt-1">{{ room.buildingName }} · {{ room.roomNumber }}</p>
            </div>
            <span class="n-rec text-[10px] text-ink-300 shrink-0">NO·{{ pad(i + 1) }}</span>
          </div>

          <div class="mt-4 space-y-2 text-sm">
            <div class="flex items-center gap-2 text-ink-500">
              <el-icon><User /></el-icon>
              <span>容纳人数：{{ room.capacity }}</span>
            </div>
            <div class="flex items-center gap-2 text-ink-500">
              <el-icon><Monitor /></el-icon>
              <span>{{ room.equipmentJson || '电脑、投影仪、空调' }}</span>
            </div>
          </div>

          <div class="mt-4 pt-3 border-t border-line/70">
            <div class="text-xs text-ink-500 mb-2">今日可预约：</div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="slot in room.availableSlots"
                :key="slot"
                :class="[
                  'n-rec text-[11px] px-2 py-0.5 rounded border',
                  slot.includes('已')
                    ? 'border-line text-ink-300 line-through'
                    : 'border-ink-300 text-ink-700',
                ]"
              >
                {{ slot }}
              </span>
            </div>
          </div>

          <el-button
            class="w-full mt-4"
            type="primary"
            @click="$router.push(`/reservation/detail/${room.id}`)"
          >
            查看时间表
          </el-button>
        </div>
      </div>
    </div>
    <div v-else class="sheet empty-state py-14">
      <strong>没有找到符合条件的场地</strong>
      <span>可以清空筛选条件后重新检索。</span>
      <el-button class="mt-4" @click="resetFilters">清空筛选</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, User, Monitor } from '@element-plus/icons-vue'
import { getResources } from '@/api/reservation'
import type { CampusResource } from '@/types/reservation'

const filters = reactive<{ date: string; resourceType: string; buildingId?: number; capacity: string }>({
  date: '', resourceType: '', buildingId: undefined, capacity: '',
})
const resources = ref<CampusResource[]>([])
const loading = ref(true)

function pad(n: number) {
  return String(n).padStart(2, '0')
}

async function fetchResources() {
  loading.value = true
  try {
    const res = await getResources({
      page: 1,
      pageSize: 20,
      resourceType: filters.resourceType || undefined,
      buildingId: filters.buildingId,
    })
    resources.value = res.data?.records || []
    if (filters.capacity) {
      resources.value = resources.value.filter((item) => {
        if (filters.capacity === 'small') return item.capacity < 30
        if (filters.capacity === 'medium') return item.capacity >= 30 && item.capacity <= 60
        return item.capacity > 60
      })
    }
  } catch (error) {
    resources.value = []
    console.error('加载场地列表失败', error)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.date = ''
  filters.resourceType = ''
  filters.buildingId = undefined
  filters.capacity = ''
  fetchResources()
}

onMounted(() => {
  fetchResources()
})
</script>
