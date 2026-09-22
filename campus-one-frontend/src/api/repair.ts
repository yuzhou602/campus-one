import request from '@/utils/request'

export function getMyRepairs(params?: any) {
  return request.get('/repairs/my', { params })
}

export function getRepairById(id: number) {
  return request.get(`/repairs/${id}`)
}

export function createRepair(data: any) {
  return request.post('/repairs', data)
}

export function updateRepairStatus(id: number, status: string) {
  return request.put(`/repairs/${id}/status`, { status })
}

export function assignRepair(id: number, userId: number) {
  return request.post(`/repairs/${id}/assign`, { userId })
}

export function getAssignedRepairs(params?: any) {
  return request.get('/repairs/assigned', { params })
}
