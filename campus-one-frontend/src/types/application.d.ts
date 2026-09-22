export interface ServiceApplication {
  id: number
  applicationNo: string
  serviceId: number
  serviceName?: string
  applicantId: number
  applicantName?: string
  formDataJson: string
  status: ApprovalStatus
  processInstanceId?: string
  currentNode?: string
  submittedAt?: string
  completedAt?: string
  createdAt: string
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
