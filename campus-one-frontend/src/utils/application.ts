import type { ApplicationFormData } from '@/types/application'

export function parseApplicationFormData(value?: string): ApplicationFormData {
  if (!value) return {}
  try {
    const parsed: unknown = JSON.parse(value)
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      return {}
    }
    return parsed as ApplicationFormData
  } catch {
    return {}
  }
}
