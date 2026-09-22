export interface CampusResource {
  id: number
  resourceCode: string
  resourceName: string
  resourceType: string
  buildingId: number
  buildingName?: string
  roomNumber: string
  capacity: number
  description?: string
  equipmentJson?: string
  status: string
  needApproval: boolean
  availableSlots?: string[]
}

export interface ResourceReservation {
  id: number
  reservationNo: string
  resourceId: number
  resourceName?: string
  userId: number
  userName?: string
  reservationDate: string
  startTime: string
  endTime: string
  purpose: string
  participantCount: number
  status: ReservationStatus
  createdAt: string
}

export interface TimeSlot {
  startTime: string
  endTime: string
  available: boolean
  reservationId?: number
}

export interface ReservationCreatePayload {
  resourceId: number
  reservationDate: string
  startTime: string
  endTime: string
  purpose: string
  attendeeCount: number
}
