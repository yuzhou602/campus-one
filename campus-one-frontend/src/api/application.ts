import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ApprovalActionPayload,
  ApprovalRecord,
  ApplicationCreatePayload,
  ApplicationQuery,
  ServiceApplication,
  ServiceCatalogItem,
} from '@/types/application'

export function getMyApplications(params?: ApplicationQuery) {
  return request.get<PageResult<ServiceApplication>>('/applications/my', { params })
}

export function getApplicationById(id: number) {
  return request.get<ServiceApplication>(`/applications/${id}`)
}

export function createApplication(data: ApplicationCreatePayload) {
  return request.post<ServiceApplication>('/applications', data)
}

export function getPendingApprovals(params?: ApplicationQuery) {
  return request.get<PageResult<ServiceApplication>>('/applications/approvals/pending', { params })
}

export function getProcessedApprovals(params?: ApplicationQuery) {
  return request.get<PageResult<ServiceApplication>>('/applications/approvals/processed', { params })
}

export function approveTask(taskId: number, data?: ApprovalActionPayload) {
  return request.post(`/applications/approvals/${taskId}/approve`, data || {})
}

export function rejectTask(taskId: number, data?: ApprovalActionPayload) {
  return request.post(`/applications/approvals/${taskId}/reject`, data || {})
}

export function getPendingApprovalsCount() {
  return request.get<number>('/applications/approvals/pending/count')
}

export function getApprovalTrail(id: number) {
  return request.get<ApprovalRecord[]>(`/applications/${id}/approvals`)
}

export function rollbackTask(id: number, comment?: string) {
  return request.post(`/applications/${id}/rollback`, { comment: comment || '' })
}

export function urgeApplication(id: number) {
  return request.post(`/applications/${id}/urge`)
}

export function getServices() {
  return request.get<ServiceCatalogItem[]>('/services')
}
