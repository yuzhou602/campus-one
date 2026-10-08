export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ServiceApplication {
  id: number
  applicationNo: string
  serviceId: number
  serviceName?: string
  applicantId: number
  applicantName?: string
  studentNo?: number
  formDataJson: string
  status: ApprovalStatus
  processInstanceId?: string
  currentNode?: string
  submittedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt?: string
  urgeCount?: number
  lastUrgedAt?: string
}

export interface ApprovalRecord {
  id: number
  applicationId: number
  taskId: string
  nodeName: string
  assigneeId: number
  assigneeName: string
  action: string
  comment?: string
  createdAt: string
}

export interface ServiceCatalogItem {
  id: number
  name: string
  description: string
  icon: string
  audience: string
  duration: string
  approvalFlow: string
  reviewRole: 'TEACHER' | 'COUNSELOR'
}

export interface ApplicationFormData {
  leaveType?: string
  startTime?: string
  endTime?: string
  reason?: string
  attachments?: Array<{ name?: string; url?: string }>
}

export interface ApplicationCreatePayload {
  serviceId: number
  title?: string
  content?: string
  formData: string
}

export interface ApplicationQuery {
  page?: number
  pageSize?: number
  status?: ApprovalStatus
}

export interface ApprovalActionPayload {
  comment?: string
}
