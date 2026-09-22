import request from '@/utils/request'

export function getMyTasks(params?: any) {
  return request.get('/tasks/my', { params })
}
