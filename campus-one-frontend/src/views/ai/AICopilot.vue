<template>
  <div class="flex flex-col h-[calc(100vh-160px)]">
    <!-- Header -->
    <div class="sheet p-6 mb-4">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-ink-900/5 border border-line flex items-center justify-center">
          <el-icon :size="24" class="text-ink-700"><MagicStick /></el-icon>
        </div>
        <div>
          <h1 class="text-xl font-semibold text-ink-900">Campus Copilot</h1>
          <p class="text-sm text-ink-500">你的 AI 校园生活与事务助手</p>
        </div>
        <span class="arch-mark n-rec ml-auto">AI · 智档</span>
      </div>

      <!-- Quick Actions -->
      <div class="flex flex-wrap gap-2 mt-4">
        <el-button
          v-for="(action, idx) in quickActions"
          :key="action.label"
          size="small"
          plain
          @click="sendMessage(action.label)"
        >
          <span class="n-rec text-[10px] text-ink-300 mr-1">{{ pad(idx + 1) }}</span>{{ action.label }}
        </el-button>
      </div>
    </div>

    <!-- Chat Messages -->
    <div ref="chatContainer" class="flex-1 overflow-y-auto space-y-4 px-2">
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center">
          <el-icon :size="48" class="text-ink-300/40"><MagicStick /></el-icon>
          <p class="text-ink-500 mt-3">输入你想了解或办理的校园事务……</p>
        </div>
      </div>

      <div v-for="(msg, idx) in messages" :key="idx" :class="['flex gap-3', msg.role === 'user' ? 'justify-end' : '']">
        <!-- AI Message -->
        <template v-if="msg.role === 'assistant'">
          <div class="w-8 h-8 rounded-lg bg-ink-900/5 border border-line flex items-center justify-center shrink-0">
            <el-icon class="text-ink-700"><MagicStick /></el-icon>
          </div>
          <div class="max-w-[70%] sheet p-4">
            <!-- Tool Call -->
            <div v-if="msg.toolCall" class="mb-3 p-2 bg-ink-900/4 rounded-lg text-xs">
              <div class="flex items-center gap-2 text-ink-700">
                <el-icon class="animate-spin"><Loading /></el-icon>
                <span>{{ msg.toolCall }}</span>
              </div>
            </div>
            <div class="ai-content text-sm text-ink-900 leading-relaxed" v-html="renderMessage(msg.content)"></div>

            <!-- Source Reference -->
            <div v-if="msg.sources" class="mt-3 pt-3 border-t border-line/70">
              <div class="text-xs text-ink-500 mb-1">参考资料：</div>
              <div v-for="(src, si) in msg.sources" :key="src" class="text-xs text-ink-700 cursor-pointer hover:underline">
                <span class="n-rec text-[10px] text-ink-300 mr-1">REF·{{ si + 1 }}</span>{{ src }}
              </div>
            </div>

            <!-- Action Card -->
            <div v-if="msg.actionCard" class="mt-3 p-3 bg-ink-900/4 rounded-lg border border-ink-900/10">
              <div class="text-xs text-ink-500 mb-2">即将执行：</div>
              <div class="text-sm text-ink-900 font-medium">{{ msg.actionCard.title }}</div>
              <div class="text-xs text-ink-500 mt-1">{{ msg.actionCard.detail }}</div>
              <div class="flex gap-2 mt-3">
                <el-button type="primary" size="small" @click="confirmAction(msg.actionCard)">确认</el-button>
                <el-button size="small" @click="cancelAction">取消</el-button>
              </div>
            </div>
          </div>
        </template>

        <!-- User Message -->
        <template v-else>
          <div class="max-w-[70%] bg-ink-900 text-white rounded-xl p-4">
            <div class="text-sm leading-relaxed whitespace-pre-wrap">{{ msg.content }}</div>
          </div>
          <div class="w-8 h-8 rounded-lg bg-ink-900 text-white flex items-center justify-center shrink-0 text-sm font-medium">
            {{ userStore.realName?.charAt(0) || 'U' }}
          </div>
        </template>
      </div>
    </div>

    <!-- Input -->
    <div class="sheet p-4 mt-4">
      <div class="flex items-end gap-3">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你想了解或办理的校园事务……"
          aria-label="向 AI 校园助手提问"
          @keydown.enter.exact.prevent="sendMessage(inputMessage)"
        />
        <el-button type="primary" aria-label="发送消息" :disabled="!inputMessage.trim() || sending" @click="sendMessage(inputMessage)">
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { MagicStick, Loading, Promotion } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDemoAiReply, isDemoMode } from '@/demo'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const userStore = useUserStore()
const inputMessage = ref('')
const sending = ref(false)
const chatContainer = ref<HTMLElement>()
const conversationId = ref('')

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  toolCall?: string
  sources?: string[]
  actionCard?: { title: string; detail: string; action: string }
}

const messages = ref<ChatMessage[]>([])
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })

function renderMessage(content: string) {
  return DOMPurify.sanitize(markdown.render(content || ''), {
    USE_PROFILES: { html: true },
  })
}

const quickActions = [
  { label: '查课表' },
  { label: '找教室' },
  { label: '查申请' },
  { label: '校园规定' },
  { label: '场地预约' },
  { label: '报修进度' },
  { label: '校园活动' },
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}

async function sendMessage(text: string) {
  const content = text.trim()
  if (!content || sending.value) return

  messages.value.push({ role: 'user', content })
  inputMessage.value = ''
  sending.value = true

  await nextTick()
  scrollToBottom()

  try {
    if (isDemoMode) {
      await new Promise(resolve => window.setTimeout(resolve, 450))
      messages.value.push({ role: 'assistant', content: getDemoAiReply(content), sources: ['CampusOne 演示数据'] })
      sending.value = false
      scrollToBottom()
      return
    }
    const response = await fetch('/api/v1/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`,
      },
      body: JSON.stringify({ message: content, conversationId: conversationId.value }),
    })

    if (!response.ok) {
      throw new Error('AI服务请求失败')
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    const assistantMessage: ChatMessage = { role: 'assistant', content: '' }
    messages.value.push(assistantMessage)

    if (reader) {
      while (true) {
        const { done, value } = await reader.read()
        buffer += decoder.decode(value, { stream: !done })
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const data = JSON.parse(line.substring(5).trim())
              if (data.content) {
                assistantMessage.content = data.content
                conversationId.value = data.conversationId
                assistantMessage.sources = data.sources
                await nextTick()
                scrollToBottom()
              }
            } catch (parseError) {
              console.warn('忽略无法解析的流式消息', parseError)
            }
          }
        }
        if (done) break
      }
    }
  } catch (error) {
    if (messages.value.at(-1)?.role === 'assistant' && !messages.value.at(-1)?.content) {
      messages.value.pop()
    }
    console.error('AI 服务请求失败', error)
    messages.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用。请稍后再试。' })
  }

  sending.value = false
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function confirmAction(card: any) {
  messages.value.push({ role: 'assistant', content: `已确认执行：${card.title}` })
  scrollToBottom()
}

function cancelAction() {
  messages.value.push({ role: 'assistant', content: '操作已取消。如需其他帮助，请随时告诉我。' })
  scrollToBottom()
}
</script>

<style scoped>
.ai-content :deep(p) { margin: 0 0 0.75rem; }
.ai-content :deep(p:last-child) { margin-bottom: 0; }
.ai-content :deep(table) { width: 100%; border-collapse: collapse; margin: 0.75rem 0; }
.ai-content :deep(th), .ai-content :deep(td) { border: 1px solid var(--color-line); padding: 0.5rem; text-align: left; }
.ai-content :deep(a) { color: var(--color-ink-900); text-decoration: underline; text-underline-offset: 3px; }
</style>
