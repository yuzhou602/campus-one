<template>
  <div class="space-y-6">
    <div class="sheet p-7">
      <div class="arch-head">
        <div>
          <h1 class="arch-title">系统管理</h1>
          <p class="arch-sub">平台配置与运维信息。</p>
        </div>
        <span class="arch-mark n-rec">SYSTEM · 总览</span>
      </div>
      <!-- 运行信息 -->
      <div class="mt-5 grid grid-cols-2 md:grid-cols-4 gap-3">
        <div v-for="inf in sysInfo" :key="inf.k" class="border border-line rounded-md px-3 py-2 bg-surface">
          <div class="text-[10px] text-ink-300 uppercase tracking-wider">{{ inf.k }}</div>
          <div class="text-[13px] text-ink-900 mt-0.5 n-rec truncate">{{ inf.v || '—' }}</div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <template v-for="(module, i) in modules" :key="module.title">
        <!-- 已建档模块：可进入 -->
        <router-link
          v-if="module.href"
          :to="module.href"
          class="sheet p-5 hover:border-ink-500 transition-colors"
        >
          <div class="flex items-center justify-between">
            <el-icon :size="24" class="text-ink-700"><component :is="module.icon" /></el-icon>
            <span class="n-rec text-[10px] text-ink-300">{{ pad(i + 1) }}</span>
          </div>
          <h3 class="text-sm font-semibold text-ink-900 mt-3">{{ module.title }}</h3>
          <p class="text-xs text-ink-500 mt-1">{{ module.desc }}</p>
          <div class="mt-3 flex items-center gap-1.5 text-[11px] text-ink-700">
            <span>进入档案 →</span>
          </div>
        </router-link>

        <!-- 待建档模块：如实标注 -->
        <div v-else class="sheet p-5 opacity-70 select-none">
          <div class="flex items-center justify-between">
            <el-icon :size="24" class="text-ink-300"><component :is="module.icon" /></el-icon>
            <span class="stamp stamp-line text-[9px]">待建档</span>
          </div>
          <h3 class="text-sm font-semibold text-ink-500 mt-3">{{ module.title }}</h3>
          <p class="text-xs text-ink-300 mt-1">{{ module.desc }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Lock, Menu, Setting, Notebook, Document } from '@element-plus/icons-vue'
import { getSystemInfo } from '@/api/system'

const sysInfo = ref<{ k: string; v: string }[]>([])

const modules = [
  { title: '用户管理', desc: '在案用户与权限', icon: User, href: '/system/users' },
  { title: '角色管理', desc: '角色与授权关系', icon: Lock },
  { title: '菜单管理', desc: '导航与功能菜单', icon: Menu },
  { title: '系统配置', desc: '平台参数配置', icon: Setting },
  { title: '字典管理', desc: '数据字典维护', icon: Notebook },
  { title: '操作日志', desc: '平台操作留痕', icon: Document },
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}

onMounted(async () => {
  try {
    const res = await getSystemInfo()
    const info = res.data || {}
    sysInfo.value = [
      { k: '平台', v: info.name || '—' },
      { k: '版本', v: info.version || '—' },
      { k: '运行时', v: info.javaVersion || '—' },
      { k: '系统', v: info.osName || '—' },
    ]
  } catch {}
})
</script>