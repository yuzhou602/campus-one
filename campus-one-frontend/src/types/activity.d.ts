export interface CampusActivity {
  id: number
  activityCode: string
  title: string
  description?: string
  category: string
  coverImage?: string
  location: string
  startTime: string
  endTime: string
  registrationDeadline: string
  capacity: number
  registeredCount: number
  organizer: string
  status: string
  createdAt: string
}

export interface ActivityRegistration {
  id: number
  activityId: number
  userId: number
  registeredAt: string
  checkedIn: boolean
  checkedInAt?: string
}
