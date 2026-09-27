<template>
  <div class="space-y-6">
    <div class="sheet p-6">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">用户管理</h1>
          <p class="arch-sub">登记在案的用户名单与权限。</p>
        </div>
        <el-button type="primary" size="small" @click="$router.push('/system')">← 返回系统管理</el-button>
      </div>
    </div>

    <div class="sheet">
      <div class="px-6 py-4 border-b border-line flex items-center justify-between gap-4 flex-wrap">
        <div class="arch-sec">在案用户</div>
        <el-input
          v-model="keyword"
          placeholder="按用户名 / 姓名检索"
          aria-label="搜索用户"
          clearable
          style="width: 220px"
          class="n-rec"
          @input="onSearch"
          @clear="onSearch"
        />
      </div>

      <div v-if="loadError && !loading" class="mx-6 mt-5 flex items-center justify-between gap-4 rounded-md border border-red-200 bg-red-50 px-4 py-3" role="alert">
        <p class="text-xs text-red-700">用户列表加载失败，请检查网络后重试。</p>
        <el-button size="small" plain @click="loadUsers">重新加载</el-button>
      </div>

      <el-table
        :data="users"
        v-loading="loading"
        :empty-text="loadError ? '加载失败' : (keyword ? '未找到匹配用户' : '暂无用户')"
        style="width: 100%"
        stripe
      >
        <el-table-column label="序号" width="60">
          <template #default="{ $index }">{{ pad(offset() + $index + 1) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column label="角色" min-width="110">
          <template #default="{ row }">
            <span class="stamp stamp-line text-[10px]">{{ roleText(row.role) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批范围" min-width="110">
          <template #default="{ row }">{{ scopeText(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" min-width="130" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <span :class="['stamp', row.status !== 0 ? '' : 'stamp-line', 'text-[10px]']">
              {{ row.status !== 0 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登记日期" min-width="160" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canConfigureScope(row)" type="primary" link @click="openScopeDialog(row)">配置范围</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="px-6 py-4 border-t border-line flex justify-end overflow-x-auto">
        <el-pagination
          background
          layout="prev, pager, next, total"
          aria-label="用户列表分页"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog v-model="scopeDialogVisible" title="配置审批数据范围" width="460px">
      <el-form label-width="100px">
        <el-form-item label="用户"><span>{{ scopeForm.realName || scopeForm.username }}</span></el-form-item>
        <el-form-item label="数据范围" required>
          <el-select v-model="scopeForm.dataScope" class="w-full">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本学院" value="COLLEGE" />
            <el-option label="本班级" value="CLASS" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="scopeForm.dataScope === 'COLLEGE'" label="学院 ID" required>
          <el-input-number v-model="scopeForm.collegeId" :min="1" :precision="0" class="w-full" />
        </el-form-item>
        <el-form-item v-if="scopeForm.dataScope === 'CLASS'" label="班级 ID" required>
          <el-input-number v-model="scopeForm.classId" :min="1" :precision="0" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scopeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scopeSaving" @click="saveScope">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, updateUserDataScope } from '@/api/system'

const users = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const loading = ref(false)
const loadError = ref(false)
const scopeDialogVisible = ref(false)
const scopeSaving = ref(false)
const scopeForm = ref<any>({ id: 0, username: '', realName: '', dataScope: 'SELF', collegeId: undefined, classId: undefined })
let searchTimer: ReturnType<typeof setTimeout> | undefined
let requestSequence = 0

function pad(n: number) {
  return String(n).padStart(2, '0')
}
function roleText(role: string) {
  const m: Record<string, string> = {
    SUPER_ADMIN: '超级管理员', ADMIN: '管理员',
    TEACHER: '教师', COUNSELOR: '辅导员',
    STUDENT: '学生', SERVICE: '服务人员',
  }
  return m[role] || role || '未知'
}
function scopeText(scope: string) {
  return ({ ALL: '全部', COLLEGE: '本学院', CLASS: '本班级', SELF: '仅本人' } as Record<string, string>)[scope] || '未配置'
}
function canConfigureScope(row: any) {
  return ['SUPER_ADMIN', 'ADMIN', 'TEACHER', 'COUNSELOR'].includes(row.role)
}
function openScopeDialog(row: any) {
  scopeForm.value = {
    id: row.id,
    username: row.username,
    realName: row.realName,
    dataScope: row.dataScope || 'SELF',
    collegeId: row.collegeId || undefined,
    classId: row.classId || undefined,
  }
  scopeDialogVisible.value = true
}
async function saveScope() {
  if (scopeForm.value.dataScope === 'COLLEGE' && !scopeForm.value.collegeId) {
    ElMessage.warning('请输入学院 ID')
    return
  }
  if (scopeForm.value.dataScope === 'CLASS' && !scopeForm.value.classId) {
    ElMessage.warning('请输入班级 ID')
    return
  }
  scopeSaving.value = true
  try {
    await updateUserDataScope(scopeForm.value.id, {
      dataScope: scopeForm.value.dataScope,
      collegeId: scopeForm.value.dataScope === 'COLLEGE' ? scopeForm.value.collegeId : undefined,
      classId: scopeForm.value.dataScope === 'CLASS' ? scopeForm.value.classId : undefined,
    })
    ElMessage.success('审批数据范围已更新')
    scopeDialogVisible.value = false
    await loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    scopeSaving.value = false
  }
}

const offset = () => (page.value - 1) * pageSize.value

async function loadUsers() {
  const sequence = ++requestSequence
  loading.value = true
  loadError.value = false
  try {
    const res: any = await getUserList({ page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    if (sequence !== requestSequence) return
    const data = res.data
    users.value = data?.records || data || []
    total.value = data?.total ?? users.value.length
  } catch (error) {
    if (sequence !== requestSequence) return
    users.value = []
    total.value = 0
    loadError.value = true
    console.error('加载用户列表失败', error)
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadUsers()
  }, 300)
}

function onPageChange(p: number) {
  page.value = p
  loadUsers()
}

onMounted(loadUsers)
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  requestSequence++
})
</script>
