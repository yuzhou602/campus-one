export interface RepairOrder {
  id: number
  repairNo: string
  userId: number
  userName?: string
  location: string
  category: string
  description: string
  priority: string
  status: RepairStatus
  assignedUserId?: number
  assignedUserName?: string
  contact: string
  availableTime?: string
  createdAt: string
  acceptedAt?: string
  resolvedAt?: string
  closedAt?: string
}

export interface RepairImage {
  id: number
  repairId: number
  imageUrl: string
  imageKey: string
}
