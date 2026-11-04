// Unwrapping of API results into data or French errors.
import { describe, expect, it } from 'vitest'
import { ApiError, errorMessage, unwrap } from './client.ts'

const response = (status: number) => new Response(null, { status })

describe('unwrap', () => {
  it('returns the data of a successful call', async () => {
    await expect(unwrap(Promise.resolve({ data: { id: 1 }, response: response(200) }))).resolves.toEqual({ id: 1 })
  })

  it('throws the detail of a problem response', async () => {
    const call = unwrap(Promise.resolve({ error: { detail: 'Liste terminée.' }, response: response(409) }))
    await expect(call).rejects.toEqual(new ApiError(409, 'Liste terminée.'))
  })

  it('replaces technical details and network failures by French messages', async () => {
    expect(errorMessage(400, 'Invalid request content.')).toBe('Données invalides : vérifiez le formulaire.')
    await expect(unwrap(Promise.reject(new TypeError('Failed to fetch')))).rejects.toMatchObject({ status: 0 })
  })
})
