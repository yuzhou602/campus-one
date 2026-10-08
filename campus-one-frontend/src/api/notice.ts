import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface NoticeItem {
  id: number
  title: string
  content?: string
  type?: string
  targetType?: string
  targetId?: number
  senderId?: number
  isRead?: boolean | number
  createdAt?: string
  publisher?: string
  publishTime?: string
  important?: boolean
  summary?: string
  source?: string
  time?: string
  unread?: boolean
  icon?: string
}

export function getNotices(params?: PageQuery & { type?: string }) {
  return request.get<PageResult<NoticeItem>>('/notices', { params })
}

export function getNoticeById(id: number) {
  return request.get<NoticeItem>(`/notices/${id}`)
}

export function markAsRead(id: number) {
  return request.put(`/notices/${id}/read`)
}

export function markAllAsRead() {
  return request.put('/notices/read-all')
}

export function getUnreadCount() {
  return request.get<number>('/notices/unread-count')
}

export function deleteNotice(id: number) {
  return request.delete(`/notices/${id}`)
}
