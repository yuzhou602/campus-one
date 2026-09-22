<template>
  <div class="min-h-screen bg-paper text-ink-900 flex">
    <!-- 纸张纹理 -->
    <div class="fixed inset-0 pointer-events-none opacity-[0.06]"
         style="background-image:radial-gradient(#221d18 1px, transparent 1px);background-size:26px 26px;" />

    <!-- 左 · 档案卷宗封面 / 目录 -->
    <aside class="hidden lg:flex lg:w-[46%] flex-col justify-between p-16 relative overflow-hidden">
      <div class="relative z-10">
        <div class="flex items-center justify-between text-[11px] uppercase tracking-[0.25em] text-ink-500">
          <span class="flex items-center gap-2.5">
            <span class="w-2 h-2 rounded-full bg-ink-900 inline-block" />
            校园综合服务档案
          </span>
          <span class="text-ink-300">档号 · CN/2026-18</span>
        </div>

        <!-- 卷宗目录条 -->
        <div class="mt-14 space-y-3">
          <div v-for="(item, i) in catalog" :key="item.no"
               class="flex items-baseline gap-4 border-b border-line/80 pb-3 reveal"
               :style="{ animationDelay: (0.08 * i + 0.1) + 's' }">
            <span class="font-mono text-[12px] text-ink-300 tabular-nums w-7">{{ pad(item.no) }}</span>
            <span class="text-[15px] font-medium tracking-wide">{{ item.name }}</span>
            <span class="ml-auto text-[11px] text-ink-300 tabular-nums">{{ item.count }} 件</span>
          </div>
        </div>
      </div>

      <div class="relative z-10">
        <h1 class="text-[2.7rem] leading-[1.12] tracking-tight font-semibold">
          每一件校园小事，<br />都该被好好归档。
        </h1>
        <div class="mt-6 flex items-center justify-between text-[11px] text-ink-300">
          <span>© 2026 CampusOne · 第一卷</span>
          <span class="tracking-widest">VOL.01</span>
        </div>
      </div>
    </aside>

    <!-- 右 · 登记凭证 -->
    <main class="flex-1 flex items-center justify-center p-8 relative z-10">
      <div class="w-full max-w-sm">
        <div class="lg:hidden flex items-center justify-between mb-8 text-[11px] uppercase tracking-[0.25em] text-ink-500">
          <span class="flex items-center gap-2.5"><span class="w-2 h-2 rounded-full bg-ink-900 inline-block" />校园档案</span>
          <span class="text-ink-300">REC · 2026-001</span>
        </div>

        <!-- 一张登记卡 -->
        <div class="card-sheet">
          <div class="flex items-center justify-between text-[11px] uppercase tracking-[0.2em] text-ink-500 border-b border-ink-900 pb-3 mb-8">
            <span>登记凭证</span>
            <span class="text-ink-300 font-mono">REC·2026-001</span>
          </div>

          <h2 class="text-3xl font-semibold tracking-tight">登记</h2>
          <p class="text-sm text-ink-500 mt-2 mb-3">请在此登记你的校园账号，继续校园日常。</p>
          <p v-if="isDemoMode" class="mb-6 rounded-md border border-ink-900/15 bg-ink-900/5 px-3 py-2 text-xs text-ink-700">
            在线演示 · 请选择下方任一账号，密码统一为 demo123
          </p>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="handleLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button native-type="submit" class="w-full login-submit" :loading="loading">
                <span class="inline-flex items-center justify-center gap-2.5">
                  <span>登 录</span>
                  <el-icon class="login-arrow" :size="15"><ArrowRight /></el-icon>
                </span>
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 档案标签 · 演示账号 -->
          <div v-if="showDemoAccounts" class="mt-8">
            <p class="text-[11px] uppercase tracking-widest text-ink-300 mb-3">快捷档案 · 演示账号</p>
            <div class="grid grid-cols-2 gap-2.5">
              <button v-for="acc in demoAccounts" :key="acc.username"
                      class="bg-surface border border-line rounded-lg px-3 py-2.5 text-left hover:border-ink-500 hover:shadow-sm transition-all cursor-pointer group"
                      @click="fillAccount(acc.username, acc.password)">
                <span class="block text-[13px] font-medium flex items-center gap-2">
                  <span class="w-1.5 h-1.5 bg-ink-900 rounded-full opacity-40 group-hover:opacity-100 transition-opacity" />
                  {{ acc.label }}
                </span>
                <span class="block text-[10px] text-ink-300 mt-0.5 font-mono">{{ acc.username }} · {{ acc.password }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { isDemoMode } from '@/demo'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const showDemoAccounts = import.meta.env.DEV || isDemoMode

const form = reactive({ username: '', password: '' })

const catalog = [
  { no: 1, name: '校园事务', count: 12 },
  { no: 2, name: '场地预约', count: 34 },
  { no: 3, name: '校园报修', count: 8 },
  { no: 4, name: '活动管理', count: 21 },
  { no: 5, name: 'AI 助手', count: 3 },
]

const demoAccounts = [
  { label: '管理员', username: 'admin', password: isDemoMode ? 'demo123' : '123456' },
  { label: '学生', username: 'student01', password: isDemoMode ? 'demo123' : '123456' },
  { label: '教师', username: 'teacher01', password: isDemoMode ? 'demo123' : '123456' },
  { label: '职工', username: 'counselor01', password: isDemoMode ? 'demo123' : '123456' },
]

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function fillAccount(username: string, password: string) {
  form.username = username
  form.password = password
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登记成功，欢迎回来！')
    router.push('/dashboard')
  } catch (err: any) {
    ElMessage.error(err.message || '登记未完成，请再试一次')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* —— 登记卡：卡纸面 + 墨线 + 极浅纸影 —— */
.card-sheet {
  position: relative;
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: 10px;
  padding: 40px 40px 34px;
  box-shadow: 0 1px 2px rgb(34 29 24 / 0.06), 0 8px 28px rgb(34 29 24 / 0.05);
  animation: sheet-in 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes sheet-in {
  from { opacity: 0; transform: translateY(16px) scale(0.99); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* —— 目录条入场（阶梯延迟） —— */
.reveal {
  opacity: 0;
  animation: rise 0.5s ease-out forwards;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* —— 登录按钮：墨黑，右侧箭头随悬停轻移 —— */
:deep(.login-submit.el-button) {
  --el-button-bg-color: var(--color-ink-900);
  --el-button-border-color: var(--color-ink-900);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: var(--color-ink-700);
  --el-button-hover-border-color: var(--color-ink-700);
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: var(--color-ink-900);
  --el-button-active-border-color: var(--color-ink-900);
  --el-button-active-text-color: #fff;
  height: 52px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: #fff;
  box-shadow: 0 1px 2px rgb(34 29 24 / 0.16);
  transition: transform 0.12s ease-out, background-color 0.2s ease-out, box-shadow 0.2s ease-out;
}
:deep(.login-submit.el-button:not(.is-loading):hover) {
  box-shadow: 0 4px 14px rgb(34 29 24 / 0.2);
}
:deep(.login-submit.el-button:not(.is-loading):active) {
  transform: translateY(1px);
  box-shadow: 0 0 0 rgb(34 29 24 / 0.1);
}
:deep(.login-submit.el-button:hover .login-arrow) {
  transform: translateX(3px);
  opacity: 1;
}
.login-arrow {
  opacity: 0.65;
  transition: transform 0.18s ease-out, opacity 0.18s ease-out;
}

/* —— 输入栏：卡纸面 + 墨线，聚焦转浓墨 —— */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #fff;
  box-shadow: inset 0 0 0 1px var(--color-line);
  transition: box-shadow 0.18s ease-out;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px var(--color-ink-300);
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgb(34 29 24 / 0.07), inset 0 0 0 1.5px var(--color-ink-900);
}
:deep(.el-form-item__error) {
  color: var(--color-danger);
}

/* —— 无障碍：减弱动效 / 可见键盘焦点 —— */
@media (prefers-reduced-motion: reduce) {
  .card-sheet, .reveal { animation: none; opacity: 1; }
  :deep(.login-submit.el-button), :deep(.el-input__wrapper) { transition: none; }
}
</style>
