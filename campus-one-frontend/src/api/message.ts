import request from '@/utils/request'

export function getMessages(params?: any) {
  return request.get('/notifications/my', { params })
}

export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllAsRead() {
  return request.put('/notifications/read-all')
}

export function deleteMessage(id: number) {
  return request.delete(`/notifications/${id}`)
}
