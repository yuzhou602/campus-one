<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <div class="sheet p-6">
      <div class="flex items-center gap-3 mb-1">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <h1 class="text-xl font-semibold text-ink-900">{{ serviceName }}</h1>
          <p class="text-sm text-ink-500">填写申请信息并提交</p>
        </div>
        <span class="arch-mark n-rec ml-auto">FORM · 新立档</span>
      </div>
    </div>

    <div class="sheet p-6">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="请假类型" prop="leaveType" v-if="serviceId === '1'">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" class="w-full">
            <el-option label="病假" value="sick" />
            <el-option label="事假" value="personal" />
            <el-option label="公假" value="official" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <div class="grid grid-cols-2 gap-4" v-if="serviceId === '1'">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" class="w-full" />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" class="w-full" />
          </el-form-item>
        </div>

        <el-form-item label="申请事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请详细描述申请事由" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            v-model:file-list="form.attachments"
            action="/api/v1/files/upload"
            :headers="uploadHeaders"
            :auto-upload="!isDemoMode"
            multiple
            :limit="5"
            accept=".jpg,.jpeg,.png,.webp,.pdf"
          >
            <el-button type="primary" plain>
              <el-icon><Upload /></el-icon>上传附件
            </el-button>
            <template #tip>
              <div class="text-xs text-ink-500 mt-1">{{ isDemoMode ? '演示模式仅记录文件名，不会上传文件' : '支持 JPG、PNG、WebP、PDF，最多 5 个文件，每个不超过 10MB' }}</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <div class="flex gap-3">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
            <el-button @click="$router.back()">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadUserFile } from 'element-plus'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { createApplication, getServices } from '@/api/application'
import type { ServiceCatalogItem } from '@/types/application'
import { isDemoMode } from '@/demo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const serviceId = computed(() => route.params.serviceId as string)
const service = ref<ServiceCatalogItem>()
const serviceName = computed(() => service.value?.name || '事务申请')

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  leaveType: '',
  startTime: '',
  endTime: '',
  reason: '',
  attachments: [] as UploadUserFile[],
})

const rules: FormRules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入申请事由', trigger: 'blur' }],
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`,
}))

async function handleSubmit() {
  if (!service.value) {
    ElMessage.error('服务事项不存在或已停用')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createApplication({
      serviceId: Number(serviceId.value),
      title: serviceName.value,
      content: '',
      formData: JSON.stringify(form),
    })
    ElMessage.success('申请已提交')
    router.push('/application/my')
  } catch (err: any) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const result = await getServices()
    service.value = (result.data || []).find(item => item.id === Number(serviceId.value))
    if (!service.value) ElMessage.error('服务事项不存在或已停用')
  } catch {
    ElMessage.error('服务目录加载失败')
  }
})
</script>
