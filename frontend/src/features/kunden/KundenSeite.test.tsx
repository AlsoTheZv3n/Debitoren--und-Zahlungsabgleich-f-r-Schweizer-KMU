import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer } from '@/mocks/demo-daten'
import { alsZeile, demoRechnung, GASSER, KELLER, seite } from '@/mocks/rechnung-daten'
import { getRechnungenSuchenMockHandler } from '@/shared/api/generiert/endpunkte/rechnungen/rechnungen.msw'
import { renderAnwendung } from '@/test/render-anwendung'

describe('Kunden', () => {
  it('sucht über die URL und öffnet die Details mit offenen Posten', async () => {
    const postenAnfragen: URLSearchParams[] = []
    const { nutzer, router } = renderAnwendung({
      pfad: '/kunden',
      handler: [
        getRechnungenSuchenMockHandler(({ request }) => {
          postenAnfragen.push(new URL(request.url).searchParams)
          return seite([alsZeile(demoRechnung())])
        }),
      ],
    })

    await nutzer.type(await screen.findByRole('searchbox', { name: 'Kunden suchen' }), 'keller')
    await waitFor(() => {
      expect(router.state.location.search).toBe('?suche=keller')
    })
    await waitFor(() => {
      expect(screen.queryByRole('link', { name: 'Marco Brunner' })).toBeNull()
    })

    await nutzer.click(screen.getByRole('link', { name: 'Keller Haustechnik AG' }))
    const drawer = await screen.findByRole('dialog', { name: 'Keller Haustechnik AG' })
    expect(router.state.location.pathname).toBe(`/kunden/${KELLER.id}`)
    expect(router.state.location.search).toBe('?suche=keller')
    expect(await within(drawer).findByRole('link', { name: 'RE-2026-0412' })).toBeInTheDocument()
    expect(within(drawer).getByText('CHF 1’840.00')).toBeInTheDocument()
    expect(postenAnfragen.at(-1)?.get('kundeId')).toBe(KELLER.id)
    expect(postenAnfragen.at(-1)?.get('offen')).toBe('true')

    await nutzer.click(within(drawer).getByRole('button', { name: 'Schliessen' }))
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/kunden')
    })
    expect(router.state.location.search).toBe('?suche=keller')
  })

  it('legt einen Kunden an und öffnet danach seine Details', async () => {
    let gesendet: unknown
    const { nutzer, router } = renderAnwendung({
      pfad: '/kunden/neu',
      handler: [
        http.post('*/api/kunden', async ({ request }) => {
          gesendet = await request.json()
          return HttpResponse.json(GASSER, { status: 201 })
        }),
      ],
    })

    const drawer = await screen.findByRole('dialog', { name: 'Neuer Kunde' })
    await nutzer.click(within(drawer).getByRole('button', { name: 'Kunde anlegen' }))
    expect(await within(drawer).findAllByText('Pflichtfeld')).toHaveLength(3)
    expect(gesendet).toBeUndefined()

    await nutzer.type(within(drawer).getByLabelText('Name'), 'Gasser Treuhand GmbH')
    await nutzer.type(within(drawer).getByLabelText('PLZ'), '6003')
    await nutzer.type(within(drawer).getByLabelText('Ort'), 'Luzern')
    await nutzer.click(within(drawer).getByRole('button', { name: 'Kunde anlegen' }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe(`/kunden/${GASSER.id}`)
    })
    expect(gesendet).toEqual({
      name: 'Gasser Treuhand GmbH',
      plz: '6003',
      ort: 'Luzern',
      land: 'CH',
      zahlungsfristTage: 30,
    })
  })

  it('zeigt Feldfehler des Backends beim passenden Feld', async () => {
    const { nutzer } = renderAnwendung({
      pfad: '/kunden/neu',
      handler: [
        http.post('*/api/kunden', () =>
          HttpResponse.json(
            {
              title: 'Kundennummer vergeben',
              status: 409,
              code: 'KUNDENNUMMER_VERGEBEN',
              detail: 'Die Kundennummer K-10001 ist bereits vergeben.',
              fehler: [{ feld: 'kundennummer', meldung: 'Bereits vergeben' }],
            },
            { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
          ),
        ),
      ],
    })

    const drawer = await screen.findByRole('dialog', { name: 'Neuer Kunde' })
    await nutzer.type(within(drawer).getByLabelText(/Kundennummer/), 'K-10001')
    await nutzer.type(within(drawer).getByLabelText('Name'), 'Doppelt AG')
    await nutzer.type(within(drawer).getByLabelText('PLZ'), '3000')
    await nutzer.type(within(drawer).getByLabelText('Ort'), 'Bern')
    await nutzer.click(within(drawer).getByRole('button', { name: 'Kunde anlegen' }))

    expect(await within(drawer).findByText('Bereits vergeben')).toBeInTheDocument()
    expect(within(drawer).getByLabelText(/Kundennummer/)).toHaveAttribute('aria-invalid', 'true')
    expect(within(drawer).getByRole('alert')).toHaveTextContent(
      'Die Kundennummer K-10001 ist bereits vergeben.',
    )
  })

  it('zeigt Lesern weder Neuanlage noch Bearbeiten', async () => {
    const { nutzer } = renderAnwendung({
      pfad: '/kunden',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    await nutzer.click(await screen.findByRole('link', { name: 'Gasser Treuhand GmbH' }))
    const drawer = await screen.findByRole('dialog', { name: 'Gasser Treuhand GmbH' })
    expect(await within(drawer).findByText('Keine offenen Rechnungen.')).toBeInTheDocument()
    expect(within(drawer).queryByRole('button', { name: 'Bearbeiten' })).toBeNull()
    expect(screen.queryByRole('link', { name: 'Neuer Kunde' })).toBeNull()
  })
})
