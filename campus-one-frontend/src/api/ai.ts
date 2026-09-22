import request from '@/utils/request'

export function chat(data: { message: string; conversationId?: string }) {
  return request.post('/ai/chat', data)
}

export function getQuickActions() {
  return request.get('/ai/quick-actions')
}

export function getKnowledgeList(params?: any) {
  return request.get('/ai/knowledge', { params })
}
