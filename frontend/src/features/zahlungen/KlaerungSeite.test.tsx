import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer } from '@/mocks/demo-daten'
import { alsZeile, demoRechnung, seite } from '@/mocks/rechnung-daten'
import { DEMO_KLAERUNG, demoZahlung } from '@/mocks/zahlung-daten'
import { getRechnungenSuchenMockHandler } from '@/shared/api/generiert/endpunkte/rechnungen/rechnungen.msw'
import { getZahlungenSuchenMockHandler } from '@/shared/api/generiert/endpunkte/zahlungen/zahlungen.msw'
import type { ZahlungDto } from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

const BRUNNER_ZAHLUNG =
  DEMO_KLAERUNG.find((zahlung) => zahlung.auftraggeber === 'M. Brunner') ?? demoZahlung()

describe('Klärungsliste', () => {
  it('ordnet den Vorschlag mit einem Klick zu, danach rückt die nächste Zahlung nach', async () => {
    let anfragen: unknown[] = []
    let offen = DEMO_KLAERUNG
    const { nutzer } = renderAnwendung({
      pfad: '/zahlungen/klaerung',
      handler: [
        getZahlungenSuchenMockHandler(() => seite(offen)),
        http.post('*/api/zahlungen/:id/zuordnungen', async ({ request }) => {
          anfragen = [...anfragen, await request.json()]
          offen = DEMO_KLAERUNG.slice(1)
          return HttpResponse.json<ZahlungDto>(
            demoZahlung({ status: 'ZUGEORDNET', klaerungsgrund: undefined, rest: 0, version: 2 }),
            { status: 201 },
          )
        }),
      ],
    })

    expect(
      await screen.findByText('3 Zahlungen liessen sich nicht automatisch zuordnen.'),
    ).toBeInTheDocument()
    const detail = screen.getByRole('region', { name: 'Keller Haustechnik' })
    expect(within(detail).getByText('Ohne Referenz')).toBeInTheDocument()
    expect(within(detail).getByText('Rechnung September')).toBeInTheDocument()
    const vorschlaege = within(detail).getByRole('region', { name: 'Vorschläge' })
    expect(
      await within(vorschlaege).findByText(
        /Betrag und Auftraggeber passen zu Rechnung RE-2026-0412/,
      ),
    ).toBeInTheDocument()

    await nutzer.click(
      within(vorschlaege).getByRole('button', { name: 'Rechnung RE-2026-0412 zuordnen' }),
    )

    expect(
      await screen.findByText(
        'Zahlung von Keller Haustechnik der Rechnung RE-2026-0412 zugeordnet.',
      ),
    ).toBeInTheDocument()
    expect(anfragen).toEqual([{ version: 1, rechnungId: demoRechnung().id }])
    expect(await screen.findByRole('region', { name: 'M. Brunner' })).toBeInTheDocument()
    expect(
      screen.getByText('2 Zahlungen liessen sich nicht automatisch zuordnen.'),
    ).toBeInTheDocument()
  })

  it('sucht eine offene Rechnung und ordnet einen Teilbetrag zu', async () => {
    let anfrage: unknown
    const { nutzer, router } = renderAnwendung({
      pfad: '/zahlungen/klaerung',
      handler: [
        getRechnungenSuchenMockHandler(({ request }) => {
          const parameter = new URL(request.url).searchParams
          expect(parameter.get('offen')).toBe('true')
          return seite(parameter.get('suche') === 'Keller' ? [alsZeile(demoRechnung())] : [])
        }),
        http.post('*/api/zahlungen/:id/zuordnungen', async ({ request }) => {
          anfrage = await request.json()
          return HttpResponse.json<ZahlungDto>(
            { ...BRUNNER_ZAHLUNG, rest: 150, klaerungsgrund: 'RESTBETRAG' },
            { status: 201 },
          )
        }),
      ],
    })

    await nutzer.click(await screen.findByRole('button', { name: /M\. Brunner/ }))
    await waitFor(() => {
      expect(router.state.location.search).toContain(`zahlung=${BRUNNER_ZAHLUNG.id}`)
    })
    const detail = screen.getByRole('region', { name: 'M. Brunner' })
    expect(within(detail).getByText('00 00000 00000 00002 02599 99991')).toBeInTheDocument()
    expect(
      within(detail).getByText('Keine Rechnung passt eindeutig; unten lässt sich suchen.'),
    ).toBeInTheDocument()

    const betrag = within(detail).getByLabelText(/Betrag/)
    await nutzer.type(betrag, '1.234')
    expect(
      within(detail).getByText('Betrag mit höchstens zwei Nachkommastellen, etwa 120.50'),
    ).toBeInTheDocument()
    await nutzer.clear(betrag)
    await nutzer.type(betrag, '100')

    await nutzer.type(
      within(detail).getByRole('searchbox', { name: 'Offene Rechnung suchen' }),
      'Keller',
    )
    const suche = within(detail).getByRole('region', { name: 'Rechnung suchen' })
    await nutzer.click(
      await within(suche).findByRole('button', { name: 'Rechnung RE-2026-0412 zuordnen' }),
    )

    expect(
      await screen.findByText(
        'Rechnung RE-2026-0412 zugeordnet; CHF 150.00 bleiben in der Klärung.',
      ),
    ).toBeInTheDocument()
    expect(anfrage).toEqual({ version: 1, rechnungId: demoRechnung().id, betrag: 100 })
  })

  it('meldet einen Konflikt, wenn jemand die Zahlung gleichzeitig bearbeitet hat', async () => {
    const { nutzer } = renderAnwendung({
      pfad: '/zahlungen/klaerung',
      handler: [
        http.post('*/api/zahlungen/:id/zuordnungen', () =>
          HttpResponse.json(
            {
              title: 'Gleichzeitig geändert',
              status: 409,
              code: 'GLEICHZEITIG_GEAENDERT',
              detail: 'Die Zahlung wurde inzwischen geändert. Bitte neu laden.',
            },
            { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
          ),
        ),
      ],
    })

    await nutzer.click(
      await screen.findByRole('button', { name: 'Rechnung RE-2026-0412 zuordnen' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Die Zahlung wurde inzwischen geändert. Bitte neu laden.',
    )
    expect(screen.getByRole('region', { name: 'Keller Haustechnik' })).toBeInTheDocument()
  })

  it('zeigt Lesern die Klärungsliste ohne Zuordnen', async () => {
    renderAnwendung({
      pfad: '/zahlungen/klaerung',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    const vorschlaege = await screen.findByRole('region', { name: 'Vorschläge' })
    expect(await within(vorschlaege).findByText('RE-2026-0412')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /zuordnen/ })).toBeNull()
    expect(screen.queryByLabelText(/Betrag/)).toBeNull()
  })

  it('zeigt einen leeren Zustand, wenn nichts zu klären ist', async () => {
    renderAnwendung({
      pfad: '/zahlungen/klaerung',
      handler: [getZahlungenSuchenMockHandler(seite([]))],
    })

    expect(await screen.findByRole('heading', { name: 'Nichts zu klären' })).toBeInTheDocument()
  })
})
