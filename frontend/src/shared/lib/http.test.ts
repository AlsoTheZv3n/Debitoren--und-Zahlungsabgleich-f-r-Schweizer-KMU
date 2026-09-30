import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import { ApiFehler, apiFetch, MANDANT_HEADER, setzeMandant, setzeTokenQuelle } from './http'

describe('apiFetch', () => {
  it('sendet Token und gewählten Mandanten mit', async () => {
    let kopf: Headers | undefined
    server.use(
      http.get('*/api/test', ({ request }) => {
        kopf = request.headers
        return HttpResponse.json({ ok: true })
      }),
    )
    setzeTokenQuelle(() => Promise.resolve('abc'))
    setzeMandant('019a0000-0000-7000-8000-000000000002')

    await expect(apiFetch('/api/test')).resolves.toEqual({ ok: true })
    expect(kopf?.get('Authorization')).toBe('Bearer abc')
    expect(kopf?.get(MANDANT_HEADER)).toBe('019a0000-0000-7000-8000-000000000002')
  })

  it('lässt Token und Mandant weg, solange keine gesetzt sind', async () => {
    let kopf: Headers | undefined
    server.use(
      http.get('*/api/test', ({ request }) => {
        kopf = request.headers
        return HttpResponse.json({})
      }),
    )

    await apiFetch('/api/test')
    expect(kopf?.has('Authorization')).toBe(false)
    expect(kopf?.has(MANDANT_HEADER)).toBe(false)
  })

  it('wirft das ProblemDetail des Backends als ApiFehler', async () => {
    server.use(
      http.get('*/api/test', () =>
        HttpResponse.json(
          {
            title: 'Mandant nicht erlaubt',
            status: 403,
            detail: 'Für diesen Mandanten fehlt die Berechtigung.',
            code: 'MANDANT_NICHT_ERLAUBT',
          },
          { status: 403, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const fehler = await apiFetch('/api/test').catch((f: unknown) => f)
    expect(fehler).toBeInstanceOf(ApiFehler)
    expect(fehler).toMatchObject({
      status: 403,
      code: 'MANDANT_NICHT_ERLAUBT',
      message: 'Für diesen Mandanten fehlt die Berechtigung.',
    })
  })

  it('macht aus Antworten ohne ProblemDetail einen allgemeinen Fehler', async () => {
    server.use(http.get('*/api/test', () => new HttpResponse('kaputt', { status: 502 })))

    await expect(apiFetch('/api/test')).rejects.toMatchObject({
      status: 502,
      code: 'UNBEKANNTER_FEHLER',
    })
  })

  it('liefert bei 204 nichts', async () => {
    server.use(http.delete('*/api/test', () => new HttpResponse(null, { status: 204 })))

    await expect(apiFetch('/api/test', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
