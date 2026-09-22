<template>
  <div class="space-y-6">
    <div class="sheet p-6">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">校园活动</h1>
          <p class="arch-sub">翻阅本学期在案的活动记录。</p>
        </div>
        <span class="arch-mark n-rec">ACT · 在档 {{ activities.length }} 场</span>
      </div>
    </div>

    <!-- 分类 · 分段控件 -->
    <div class="sheet px-6 py-3">
      <el-radio-group v-model="activeCategory">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="academic">学术</el-radio-button>
        <el-radio-button value="competition">竞赛</el-radio-button>
        <el-radio-button value="lecture">讲座</el-radio-button>
        <el-radio-button value="club">社团</el-radio-button>
        <el-radio-button value="sports">体育</el-radio-button>
        <el-radio-button value="volunteer">志愿服务</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 活动档案卡 -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="activity in activities"
        :key="activity.id"
        class="sheet cursor-pointer hover:border-ink-500 transition-colors overflow-hidden group/item"
        @click="$router.push(`/activity/detail/${activity.id}`)"
      >
        <div class="px-5 pt-5 flex items-start justify-between">
          <span class="arch-sec text-[10px]">活动登记</span>
          <span class="stamp stamp-line text-[10px]">{{ activity.categoryName || '未分类' }}</span>
        </div>
        <div class="p-5 pt-4">
          <h3 class="text-base font-semibold text-ink-900 line-clamp-1">{{ activity.title }}</h3>
          <div class="mt-3 space-y-1.5 text-xs text-ink-500">
            <div class="flex items-center gap-1.5">
              <el-icon :size="13"><Clock /></el-icon>
              <span>{{ activity.startTime }}</span>
            </div>
            <div class="flex items-center gap-1.5">
              <el-icon :size="13"><Location /></el-icon>
              <span>{{ activity.location }}</span>
            </div>
          </div>
          <div class="mt-4 pt-3 border-t border-line/70">
            <div class="flex items-center justify-between">
              <span class="text-xs text-ink-500">{{ activity.organizer }}</span>
              <span v-if="activity.registered >= activity.capacity" class="stamp text-[10px]">已满 </span>
              <span v-else class="n-rec text-xs">{{ activity.registered }}/{{ activity.capacity }} 人</span>
            </div>
            <el-progress
              :percentage="Math.round((activity.registered / activity.capacity) * 100)"
              :stroke-width="4"
              color="#221D18"
              class="mt-2"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Clock, Location } from '@element-plus/icons-vue'
import { getActivities } from '@/api/activity'

const activeCategory = ref('all')
const activities = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getActivities({ page: 1, pageSize: 20 })
    activities.value = res.data?.records || res.data || []
  } catch {} finally {
    loading.value = false
  }
})
</script>