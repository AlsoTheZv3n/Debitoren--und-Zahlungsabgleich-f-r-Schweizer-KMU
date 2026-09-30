import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer } from '@/mocks/demo-daten'
import { alsZeile, BRUNNER, demoEntwurf, demoRechnung, seite } from '@/mocks/rechnung-daten'
import { getRechnungenSuchenMockHandler } from '@/shared/api/generiert/endpunkte/rechnungen/rechnungen.msw'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

const ENTWURF_KELLER = demoEntwurf()
const ENTWURF_BRUNNER = demoEntwurf({
  id: '019a2000-0000-7000-8000-000000000998',
  version: 4,
  kundeId: BRUNNER.id,
  empfaenger: { name: BRUNNER.name, plz: BRUNNER.plz, ort: BRUNNER.ort, land: 'CH' },
})
const OFFENE_RECHNUNG = demoRechnung()

describe('Rechnungsliste', () => {
  it('gibt die gewählten Entwürfe gesammelt frei, jeden mit seiner Version', async () => {
    const freigaben: unknown[] = []
    let freigegeben = false
    const { nutzer } = renderAnwendung({
      pfad: '/rechnungen',
      handler: [
        getRechnungenSuchenMockHandler(() =>
          seite(
            (freigegeben
              ? [OFFENE_RECHNUNG]
              : [ENTWURF_KELLER, ENTWURF_BRUNNER, OFFENE_RECHNUNG]
            ).map(alsZeile),
          ),
        ),
        http.post('*/api/rechnungen/:id/freigabe', async ({ params, request }) => {
          freigaben.push({ id: params.id, ...((await request.json()) as object) })
          freigegeben = true
          return HttpResponse.json<RechnungDto>(demoRechnung({ id: String(params.id) }))
        }),
      ],
    })

    expect(await screen.findByRole('link', { name: 'RE-2026-0412' })).toBeInTheDocument()
    expect(screen.queryByRole('checkbox', { name: /RE-2026-0412/ })).toBeNull()
    expect(
      screen.getByRole('checkbox', { name: 'Entwurf an Marco Brunner auswählen' }),
    ).toBeEnabled()

    await nutzer.click(
      screen.getByRole('checkbox', { name: 'Alle Entwürfe auf dieser Seite auswählen' }),
    )
    const aktionen = screen.getByRole('region', { name: 'Sammelaktionen' })
    expect(within(aktionen).getByText('2 Entwürfe gewählt')).toBeInTheDocument()

    await nutzer.click(within(aktionen).getByRole('button', { name: 'Freigeben' }))
    const rueckfrage = await screen.findByRole('dialog', { name: '2 Entwürfe freigeben?' })
    await nutzer.click(within(rueckfrage).getByRole('button', { name: 'Freigeben' }))

    expect(await screen.findByText('2 Rechnungen sind freigegeben.')).toBeInTheDocument()
    expect(freigaben).toEqual([
      { id: ENTWURF_KELLER.id, version: 0 },
      { id: ENTWURF_BRUNNER.id, version: 4 },
    ])
    expect(screen.queryByRole('region', { name: 'Sammelaktionen' })).toBeNull()
  })

  it('meldet Entwürfe, die das Backend nicht freigibt, einzeln', async () => {
    const { nutzer } = renderAnwendung({
      pfad: '/rechnungen?status=ENTWURF',
      handler: [
        getRechnungenSuchenMockHandler(seite([alsZeile(ENTWURF_BRUNNER)])),
        http.post('*/api/rechnungen/:id/freigabe', () =>
          HttpResponse.json(
            {
              title: 'Freigabe nicht möglich',
              status: 409,
              code: 'FREIGABE_NICHT_MOEGLICH',
              detail: 'Eine Rechnung braucht mindestens eine Position und einen Betrag über null.',
            },
            { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
          ),
        ),
      ],
    })

    await nutzer.click(
      await screen.findByRole('checkbox', { name: 'Entwurf an Marco Brunner auswählen' }),
    )
    await nutzer.click(screen.getByRole('button', { name: 'Freigeben' }))
    const rueckfrage = await screen.findByRole('dialog', { name: 'Einen Entwurf freigeben?' })
    await nutzer.click(within(rueckfrage).getByRole('button', { name: 'Freigeben' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Entwurf an Marco Brunner: Eine Rechnung braucht mindestens eine Position und einen Betrag über null.',
    )
  })

  it('hält Filter in der URL und fragt das Backend damit ab', async () => {
    const anfragen: URLSearchParams[] = []
    const { nutzer, router } = renderAnwendung({
      pfad: '/rechnungen',
      handler: [
        getRechnungenSuchenMockHandler(({ request }) => {
          anfragen.push(new URL(request.url).searchParams)
          return seite([])
        }),
      ],
    })

    expect(await screen.findByText('Noch keine Rechnungen')).toBeInTheDocument()
    await nutzer.selectOptions(screen.getByLabelText('Status'), 'OFFEN')
    await nutzer.click(screen.getByLabelText('Nur überfällige'))

    await waitFor(() => {
      expect(router.state.location.search).toBe('?status=OFFEN&ueberfaellig=ja')
    })
    expect(await screen.findByText('Keine Treffer')).toBeInTheDocument()
    expect(anfragen.at(-1)?.get('status')).toBe('OFFEN')
    expect(anfragen.at(-1)?.get('ueberfaellig')).toBe('true')
  })

  it('zeigt Lesern keine Auswahl und keine neue Rechnung', async () => {
    renderAnwendung({
      pfad: '/rechnungen',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    expect(await screen.findByRole('link', { name: 'RE-2026-0412' })).toBeInTheDocument()
    expect(screen.queryByRole('checkbox', { name: /auswählen/ })).toBeNull()
    expect(screen.queryByRole('link', { name: 'Neue Rechnung' })).toBeNull()
  })
})
