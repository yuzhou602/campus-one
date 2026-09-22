<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3 mb-4">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="flex-1">
          <div class="flex items-center gap-3">
            <h1 class="text-xl font-semibold text-ink-900">{{ application.title || '申请详情' }}</h1>
            <span v-if="reviewGroup" class="stamp text-[10px]">{{ reviewGroup }} · 负责审批</span>
          </div>
          <p class="text-sm text-ink-500 n-rec mt-1">{{ application.applicationNo || '' }}</p>
        </div>
        <StatusTag :status="application.status || 'PROCESSING'" type="approval" size="large" />
        <el-button
          v-if="application.status === 'PENDING'"
          size="small" text
          :disabled="urging"
          @click="handleUrge"
        >催办 · {{ application.urgeCount || 0 }} 次</el-button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Application Info -->
      <div class="lg:col-span-2 sheet p-6">
        <h2 class="arch-sec mb-4">基本信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-ink-500">申请人：</span><span class="text-ink-900">{{ application.applicantName || '-' }}</span></div>
          <div><span class="text-ink-500">学号：</span><span class="text-ink-900">{{ application.studentNo || '-' }}</span></div>
          <div><span class="text-ink-500">学院：</span><span class="text-ink-900">{{ application.department || '-' }}</span></div>
          <div><span class="text-ink-500">班级：</span><span class="text-ink-900">{{ application.className || '-' }}</span></div>
          <div><span class="text-ink-500">请假类型：</span><span class="text-ink-900">{{ application.leaveType || '-' }}</span></div>
          <div><span class="text-ink-500">开始时间：</span><span class="text-ink-900">{{ application.startTime || '-' }}</span></div>
          <div><span class="text-ink-500">结束时间：</span><span class="text-ink-900">{{ application.endTime || '-' }}</span></div>
          <div><span class="text-ink-500">联系电话：</span><span class="text-ink-900">{{ application.phone || '-' }}</span></div>
        </div>
        <div class="mt-4">
          <span class="text-ink-500 text-sm">请假原因：</span>
          <p class="text-sm text-ink-900 mt-1">{{ application.reason || '-' }}</p>
        </div>
      </div>

      <!-- Approval Progress -->
      <div class="sheet p-6">
        <h2 class="arch-sec mb-4">审批进度</h2>
        <div class="space-y-4">
          <div v-for="(step, i) in steps" :key="i" class="flex items-start gap-3">
            <div :class="[
                'w-6 h-6 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5',
                step.done ? 'bg-ink-900 text-white'
                  : step.active ? 'bg-ink-900/10 text-ink-900'
                  : 'bg-ink-300/30 text-ink-300',
              ]">
              {{ step.done ? '✓' : step.active ? '●' : '○' }}
            </div>
            <div>
              <div :class="['text-sm font-medium', step.done || step.active ? 'text-ink-900' : 'text-ink-500']">
                {{ step.label }}
              </div>
              <div :class="['text-xs mt-0.5', step.done || step.active ? 'text-ink-500' : 'text-ink-300']">
                {{ step.text }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 审批卷宗附页：所有审批意见按时间倒序留痕 -->
    <div class="sheet p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="arch-sec mb-0">审批卷宗附页</h2>
        <span class="n-rec text-[10px] text-ink-300">{{ trail.length }} 则</span>
      </div>
      <div v-if="trail.length" class="divide-y divide-line">
        <div v-for="rec in trail" :key="rec.id" class="py-3 flex items-start gap-3">
          <span :class="['stamp shrink-0 text-[10px] mt-0.5', rec.action === 'APPROVED' ? '' : 'stamp-line']">
            {{ trailActionText(rec.action) }}
          </span>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-ink-900">
              {{ rec.nodeName || '审批节点' }}
              <span class="text-ink-300 font-normal">· {{ rec.assigneeName || '审批人' }}</span>
            </div>
            <div v-if="rec.comment" class="text-sm text-ink-700 mt-0.5">{{ rec.comment }}</div>
            <div v-else class="text-xs text-ink-300 mt-0.5">（未留审批意见）</div>
          </div>
          <span class="n-rec text-[11px] text-ink-300">{{ formatTime(rec.createdAt) }}</span>
        </div>
      </div>
      <div v-else class="text-sm text-ink-500 py-4">尚未产生审批意见。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/common/StatusTag.vue'
import { getApplicationById, getApprovalTrail, urgeApplication } from '@/api/application'

const route = useRoute()
const application = ref<any>({})
const trail = ref<any[]>([])
const loading = ref(false)
const urging = ref(false)

function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}月${p(d.getDate())}日 ${p(d.getHours())}:${p(d.getMinutes())}`
}
function trailActionText(action?: string) {
  const m: Record<string, string> = {
    APPROVED: '通过', REJECTED: '驳回', ROLLED_BACK: '退回',
  }
  return m[action || ''] || action || '节点'
}

async function handleUrge() {
  urging.value = true
  try {
    await urgeApplication(application.value.id)
    application.value.urgeCount = (application.value.urgeCount || 0) + 1
    ElMessage.success('已催促当前审批人')
  } catch (e: any) {
    ElMessage.error(e.message || '催办失败')
  } finally { urging.value = false }
}

/** 由后端写回的当前审批节点识别负责群体 */
const reviewGroup = computed(() => {
  const n = application.value.currentNode || ''
  if (n.includes('教师')) return '教师'
  if (n.includes('职工')) return '职工'
  return ''
})

/** 审批进度：随申请状态与负责群体真实推进 */
const steps = computed(() => {
  const st = application.value.status
  const group = reviewGroup.value || '负责'
  const submitting = ['PENDING', 'SUBMITTED', 'REJECTED'].includes(st)
  const reviewLabel = application.value.currentNode || `${group}审批`
  return [
    { label: '提交申请', done: true, active: false, text: '已提交' },
    {
      label: reviewLabel,
      done: st === 'APPROVED',
      active: submitting,
      text: st === 'REJECTED' ? '已驳回' : (submitting ? '处理中' : '已通过'),
    },
    { label: '终审归档', done: st === 'APPROVED', active: false, text: st === 'APPROVED' ? '已完成' : '待审' },
  ]
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getApplicationById(id)
    application.value = res.data || {}
  } catch {} finally { loading.value = false }
  try {
    const t = await getApprovalTrail(id)
    trail.value = (t.data as any) || []
  } catch {}
})
</script>
