<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3 mb-4">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div>
          <h1 class="text-xl font-semibold text-ink-900">{{ resource?.resourceName || '场地详情' }}</h1>
          <p class="text-sm text-ink-500">{{ resource?.buildingName || '' }} {{ resource?.roomNumber || '' }} · {{ resource?.capacity ? `容纳${resource.capacity}人` : '' }}</p>
        </div>
      </div>
    </div>

    <!-- Calendar / Timeline -->
    <div class="sheet p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="arch-sec">选择预约时间</h2>
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disablePastDate" placeholder="选择日期" aria-label="预约日期" size="default" />
      </div>

      <div class="space-y-2">
        <div
          v-for="slot in timeSlots"
          :key="`${slot.startTime}-${slot.endTime}`"
          :class="[
            'flex items-center justify-between p-3 rounded-lg border transition-all',
            slot.available && selectedSlot !== slot
              ? 'border-ink-300 bg-surface hover:border-ink-900 cursor-pointer'
              : selectedSlot === slot
                ? 'border-ink-900 bg-ink-900/5'
                : 'border-line bg-ink-900/2 opacity-60',
          ]"
          @click="slot.available && selectSlot(slot)"
        >
          <div class="flex items-center gap-3">
            <div :class="['w-3 h-3 rounded-full', slot.available ? 'bg-ink-900' : 'bg-ink-300/60']"></div>
            <span class="text-sm font-medium text-ink-900">{{ slot.startTime }} - {{ slot.endTime }}</span>
          </div>
          <span :class="['text-xs', slot.available ? 'text-ink-700' : 'text-ink-500']">
            {{ selectedSlot === slot ? '已选择' : slot.available ? '可预约' : '已占用' }}
          </span>
        </div>
      </div>
    </div>

    <!-- Booking Form -->
    <div v-if="selectedSlot" class="sheet p-6">
      <h2 class="arch-sec mb-4">预约信息</h2>
      <el-form :model="bookingForm" label-position="top" size="large">
        <el-form-item label="预约时段">
          <el-tag type="primary" size="large">{{ selectedDate }} {{ selectedSlot.startTime }} - {{ selectedSlot.endTime }}</el-tag>
        </el-form-item>
        <el-form-item label="使用目的">
          <el-input v-model="bookingForm.purpose" type="textarea" :rows="3" placeholder="请输入使用目的" />
        </el-form-item>
        <el-form-item label="参与人数">
          <el-input-number v-model="bookingForm.attendeeCount" :min="1" :max="resource?.capacity || 1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleBooking">
            确认预约
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createReservation, getResourceById, getAvailability } from '@/api/reservation'
import type { CampusResource, TimeSlot } from '@/types/reservation'
import dayjs from 'dayjs'

const route = useRoute()
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedSlot = ref<TimeSlot | null>(null)
const submitting = ref(false)
const resource = ref<CampusResource | null>(null)
const loading = ref(false)

const timeSlots = ref<TimeSlot[]>([])

const bookingForm = reactive({
  purpose: '',
  attendeeCount: 1,
})

function disablePastDate(date: Date) {
  return dayjs(date).isBefore(dayjs().startOf('day'))
}

async function loadAvailability() {
  if (!resource.value || !selectedDate.value) return
  selectedSlot.value = null
  try {
    const availRes = await getAvailability(resource.value.id, selectedDate.value)
    timeSlots.value = availRes.data || []
  } catch (error) {
    timeSlots.value = []
    console.error('加载可预约时段失败', error)
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await getResourceById(id)
    resource.value = res.data
    await loadAvailability()
  } catch (error) {
    console.error('加载场地详情失败', error)
  } finally { loading.value = false }
})

watch(selectedDate, loadAvailability)

function selectSlot(slot: TimeSlot) {
  selectedSlot.value = slot
}

async function handleBooking() {
  if (!bookingForm.purpose) {
    ElMessage.warning('请输入使用目的')
    return
  }
  submitting.value = true
  try {
    await createReservation({
      resourceId: resource.value!.id,
      reservationDate: selectedDate.value,
      startTime: selectedSlot.value!.startTime,
      endTime: selectedSlot.value!.endTime,
      purpose: bookingForm.purpose.trim(),
      attendeeCount: bookingForm.attendeeCount,
    })
    ElMessage.success('预约成功！')
  } catch (err: any) {
    ElMessage.error(err.message || '预约失败')
  } finally {
    submitting.value = false
  }
}
</script>
