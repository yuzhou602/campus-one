export type ApprovalStatus = 'DRAFT' | 'SUBMITTED' | 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'CANCELLED'

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'IN_USE' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED' | 'REJECTED'

export type RepairStatus = 'SUBMITTED' | 'ASSIGNED' | 'ACCEPTED' | 'PROCESSING' | 'WAITING_PART' | 'RESOLVED' | 'CONFIRMED' | 'CLOSED' | 'CANCELLED'

export type UserRole = 'STUDENT' | 'TEACHER' | 'COUNSELOR' | 'SERVICE' | 'ADMIN' | 'SUPER_ADMIN'

export interface SelectOption {
  label: string
  value: string | number
}
