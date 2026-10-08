import { describe, expect, it } from 'vitest'
import { isNoticeRead } from './notice'

describe('isNoticeRead', () => {
  it.each([true, 1] as const)('treats %s as read', value => {
    expect(isNoticeRead(value)).toBe(true)
  })

  it.each([false, 0, undefined] as const)('treats %s as unread', value => {
    expect(isNoticeRead(value)).toBe(false)
  })
})
