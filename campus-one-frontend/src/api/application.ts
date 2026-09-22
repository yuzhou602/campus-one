import request from '@/utils/request'

export function getMyApplications(params?: any) {
  return request.get('/applications/my', { params })
}

export function getApplicationById(id: number) {
  return request.get(`/applications/${id}`)
}

export function submitApplication(data: any) {
  return request.post('/applications', data)
}

export function createApplication(data: any) {
  return request.post('/applications', data)
}

export function getPendingApprovals(params?: any) {
  return request.get('/applications/approvals/pending', { params })
}

export function getProcessedApprovals(params?: any) {
  return request.get('/applications/approvals/processed', { params })
}

export function approveTask(taskId: number, data?: any) {
  return request.post(`/applications/approvals/${taskId}/approve`, data || {})
}

export function rejectTask(taskId: number, data?: any) {
  return request.post(`/applications/approvals/${taskId}/reject`, data || {})
}

export function getPendingApprovalsCount() {
  return request.get('/applications/approvals/pending/count')
}

export function getApprovalTrail(id: number) {
  return request.get(`/applications/${id}/approvals`)
}

export function rollbackTask(id: number, comment?: string) {
  return request.post(`/applications/${id}/rollback`, { comment: comment || '' })
}

export function urgeApplication(id: number) {
  return request.post(`/applications/${id}/urge`)
}

export function getServices() {
  return request.get('/services')
}
