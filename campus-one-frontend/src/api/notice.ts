import request from '@/utils/request'

export function getNotices(params?: any) {
  return request.get('/notices', { params })
}

export function getNoticeById(id: number) {
  return request.get(`/notifications/${id}`)
}

export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllAsRead() {
  return request.put('/notifications/read-all')
}

export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}
