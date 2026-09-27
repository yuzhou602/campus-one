import request from '@/utils/request'

export function getUserList(params?: any) {
  return request.get('/users', { params })
}

export function getSystemInfo() {
  return request.get('/system/info')
}

export function updateUserDataScope(id: number, data: { dataScope: string; collegeId?: number; classId?: number }) {
  return request.put(`/users/${id}/data-scope`, data)
}
