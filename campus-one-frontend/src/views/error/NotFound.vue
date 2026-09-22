<template>
  <div class="min-h-screen flex items-center justify-center" style="background: var(--color-paper)">
    <div class="text-center px-8">
      <div class="text-8xl font-bold tracking-tight" style="color: var(--color-ink-300)">404</div>
      <h1 class="text-xl font-semibold mt-4" style="color: var(--color-ink-900)">该卷宗不在档</h1>
      <p class="text-sm mt-2" style="color: var(--color-ink-500)">你访问的页面不存在或已被移除</p>
      <el-button type="primary" class="mt-6" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>