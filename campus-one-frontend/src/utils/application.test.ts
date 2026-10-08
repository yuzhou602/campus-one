import { describe, expect, it } from 'vitest'
import { parseApplicationFormData } from './application'

describe('parseApplicationFormData', () => {
  it('parses a valid application form object', () => {
    expect(parseApplicationFormData('{"leaveType":"sick","reason":"发烧"}')).toEqual({
      leaveType: 'sick',
      reason: '发烧',
    })
  })

  it.each([undefined, '', 'not-json', 'null', '[]', '42', '"text"'])(
    'returns an empty object for unsafe input: %s',
    value => {
      expect(parseApplicationFormData(value)).toEqual({})
    },
  )
})
