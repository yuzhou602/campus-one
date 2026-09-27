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

export function acceptRepair(id: number) {
  return request.post(`/repairs/${id}/accept`)
}

export function assignRepair(id: number, userId: number) {
  return request.post(`/repairs/${id}/assign`, { userId })
}

export function getAssignedRepairs(params?: any) {
  return request.get('/repairs/assigned', { params })
}

export function getUnassignedRepairs(params?: any) {
  return request.get('/repairs/unassigned', { params })
}

export function getRepairTechnicians() {
  return request.get<Array<{ id: number; username: string; realName: string }>>('/repairs/technicians')
}
