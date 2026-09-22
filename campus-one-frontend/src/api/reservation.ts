import request from '@/utils/request'
import type { CampusResource, ReservationCreatePayload, ResourceReservation, TimeSlot } from '@/types/reservation'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

interface ResourceQuery {
  resourceType?: string
  buildingId?: number
  page?: number
  pageSize?: number
}

export function getResources(params?: ResourceQuery) {
  return request.get<PageResult<CampusResource>>('/reservations/resources', { params })
}

export function getResourceById(id: number) {
  return request.get<CampusResource>(`/resources/${id}`)
}

export function getReservations(params?: any) {
  return request.get('/reservations/my', { params })
}

export function getReservationById(id: number) {
  return request.get<ResourceReservation>(`/reservations/${id}`)
}

export function createReservation(data: ReservationCreatePayload) {
  return request.post<ResourceReservation>('/reservations', data)
}

export function cancelReservation(id: number) {
  return request.put(`/reservations/${id}/cancel`)
}

export function getAvailability(resourceId: number, date: string) {
  return request.get<TimeSlot[]>('/reservations/availability', { params: { resourceId, date } })
}
