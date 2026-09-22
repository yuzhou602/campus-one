<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">校园报修</h1>
          <p class="arch-sub">提交报修工单，快速解决校园设施问题。</p>
        </div>
        <span class="arch-mark n-rec">FIX · 即时登记</span>
      </div>
      <div class="mt-4">
        <el-button type="primary" @click="showSubmitDialog = true">
          <el-icon><Plus /></el-icon>提交报修
        </el-button>
      </div>
    </div>

    <!-- Repair Categories -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div
        v-for="(cat, i) in categories"
        :key="cat.value"
        class="sheet p-4 hover:border-ink-500 transition-colors cursor-pointer text-center"
        @click="filterByCategory(cat.value)"
      >
        <el-icon :size="24" class="text-ink-700"><component :is="cat.icon" /></el-icon>
        <div class="text-sm font-medium text-ink-900 mt-2">{{ cat.label }}</div>
        <div class="n-rec text-[10px] text-ink-300 mt-1">NO·{{ pad(i + 1) }}</div>
      </div>
    </div>

    <!-- Submit Dialog -->
    <el-dialog v-model="showSubmitDialog" title="提交报修" width="600px" destroy-on-close>
      <el-form ref="repairFormRef" :model="repairForm" :rules="repairRules" label-position="top" size="large">
        <el-form-item label="报修位置" prop="location">
          <el-input v-model="repairForm.location" placeholder="如：信息楼305、学生宿舍12号楼301" />
        </el-form-item>
        <el-form-item label="故障类型" prop="category">
          <el-select v-model="repairForm.category" placeholder="请选择故障类型" class="w-full">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="description">
          <el-input v-model="repairForm.description" type="textarea" :rows="4" placeholder="请详细描述故障情况" />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload v-model:file-list="repairForm.images" action="#" :auto-upload="false" list-type="picture-card" :limit="5">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="repairForm.contact" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="可维修时间">
          <el-input v-model="repairForm.availableTime" placeholder="如：工作日白天、周末全天" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitRepair">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Plus, HomeFilled, Monitor, Connection, Setting } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createRepair } from '@/api/repair'

const showSubmitDialog = ref(false)
const submitting = ref(false)
const repairFormRef = ref<FormInstance>()

const categories = [
  { label: '宿舍维修', value: 'dorm', icon: HomeFilled },
  { label: '教室设备', value: 'classroom', icon: Monitor },
  { label: '水电维修', value: 'water', icon: Connection },
  { label: '网络故障', value: 'network', icon: Setting },
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}

const repairForm = reactive({
  location: '',
  category: '',
  description: '',
  images: [] as any[],
  contact: '',
  availableTime: '',
})

const repairRules: FormRules = {
  location: [{ required: true, message: '请输入报修位置', trigger: 'blur' }],
  category: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  description: [{ required: true, message: '请描述故障情况', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
}

function filterByCategory(value: string) {
  repairForm.category = value
  showSubmitDialog.value = true
}

async function handleSubmitRepair() {
  const valid = await repairFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createRepair(repairForm)
    ElMessage.success('报修工单已提交')
    showSubmitDialog.value = false
  } catch (err: any) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
