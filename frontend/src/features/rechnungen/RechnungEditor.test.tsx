import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { demoEntwurf, demoRechnung, demoVorschau, KELLER } from '@/mocks/rechnung-daten'
import type { RechnungDto, VorschauAnfrageDto } from '@/shared/api/generiert/modell'
import { heuteIso } from '@/shared/lib/format'
import { renderAnwendung } from '@/test/render-anwendung'

describe('Rechnungseditor', () => {
  it('erfasst eine Rechnung mit Live-Vorschau und speichert sie als Entwurf', async () => {
    const vorschauen: VorschauAnfrageDto[] = []
    let gesendet: unknown
    const { nutzer, router } = renderAnwendung({
      pfad: `/rechnungen/neu?kunde=${KELLER.id}`,
      handler: [
        http.post('*/api/rechnungen/vorschau', async ({ request }) => {
          vorschauen.push((await request.json()) as VorschauAnfrageDto)
          return HttpResponse.json(demoVorschau({ positionsbetraege: [942.5] }))
        }),
        http.post('*/api/rechnungen', async ({ request }) => {
          gesendet = await request.json()
          return HttpResponse.json(demoEntwurf(), { status: 201 })
        }),
      ],
    })

    expect(await screen.findByRole('heading', { name: 'Neue Rechnung' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Kunde' })).toHaveValue(
      'Keller Haustechnik AG (K-10001)',
    )
    expect(screen.getByLabelText('Rechnungsdatum')).toHaveValue(heuteIso())
    expect(screen.getByText('Leer lassen für 30 Tage wie beim Kunden')).toBeInTheDocument()

    await nutzer.type(screen.getByLabelText('Beschreibung, Position 1'), 'Service Heizungsanlage')
    await nutzer.clear(screen.getByLabelText('Menge, Position 1'))
    await nutzer.type(screen.getByLabelText('Menge, Position 1'), '6,5')
    await nutzer.type(screen.getByLabelText('Einzelpreis, Position 1'), '145')

    const summen = screen.getByRole('region', { name: 'Summen' })
    expect(
      await screen.findByRole('img', { name: 'Vorschau des QR-Zahlteils' }),
    ).toBeInTheDocument()
    expect(within(summen).getByText('CHF 1’840.00')).toBeInTheDocument()
    await waitFor(() => {
      expect(vorschauen.at(-1)?.positionen).toEqual([
        {
          beschreibung: 'Service Heizungsanlage',
          menge: 6.5,
          einzelpreis: 145,
          mwstCode: 'NORMAL',
        },
      ])
    })
    expect(vorschauen.at(-1)?.kundeId).toBe(KELLER.id)
    expect(await screen.findByText('CHF 942.50')).toBeInTheDocument()

    await nutzer.click(screen.getByRole('button', { name: 'Entwurf speichern' }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe(`/rechnungen/${demoEntwurf().id}`)
    })
    expect(await screen.findByText('Entwurf gespeichert.')).toBeInTheDocument()
    expect(gesendet).toEqual({
      kundeId: KELLER.id,
      rechnungsdatum: heuteIso(),
      waehrung: 'CHF',
      positionen: [
        {
          beschreibung: 'Service Heizungsanlage',
          menge: 6.5,
          einzelpreis: 145,
          mwstCode: 'NORMAL',
        },
      ],
    })
  })

  it('wählt den Kunden per Tastatur und prüft vor der Freigabe alle Felder', async () => {
    const { nutzer } = renderAnwendung({ pfad: '/rechnungen/neu' })

    const kunde = await screen.findByRole('combobox', { name: 'Kunde' })
    await nutzer.click(screen.getByRole('button', { name: 'Freigeben' }))
    expect(await screen.findByText('Kunde wählen')).toBeInTheDocument()
    expect(screen.getAllByText('Pflichtfeld')).toHaveLength(2)
    expect(screen.queryByRole('dialog')).toBeNull()

    await nutzer.type(kunde, 'Brun')
    // Enter übernimmt erst den Vorschlag, der zur Eingabe gehört.
    expect(
      await screen.findByRole('option', { name: /Marco Brunner/, selected: true }),
    ).toBeInTheDocument()
    await nutzer.keyboard('{Enter}')

    expect(kunde).toHaveValue('Marco Brunner (K-10002)')
    expect(screen.queryByText('Kunde wählen')).toBeNull()
    expect(screen.getByText('Leer lassen für 10 Tage wie beim Kunden')).toBeInTheDocument()
  })

  it('gibt einen gespeicherten Entwurf nach Rückfrage frei und zeigt dann die Rechnung', async () => {
    let aktuell: RechnungDto = demoEntwurf()
    let freigabe: unknown
    const { nutzer } = renderAnwendung({
      pfad: `/rechnungen/${aktuell.id}`,
      handler: [
        http.get('*/api/rechnungen/:id', () => HttpResponse.json(aktuell)),
        http.post('*/api/rechnungen/:id/freigabe', async ({ request }) => {
          freigabe = await request.json()
          aktuell = demoRechnung({ id: aktuell.id, version: 1 })
          return HttpResponse.json(aktuell)
        }),
      ],
    })

    expect(await screen.findByRole('heading', { name: 'Rechnungsentwurf' })).toBeInTheDocument()
    expect(screen.getByLabelText('Beschreibung, Position 2')).toHaveValue('Umwälzpumpe Grundfos')
    expect(screen.getByLabelText('Einzelpreis, Position 2')).toHaveValue('759.63')

    await nutzer.click(screen.getByRole('button', { name: 'Freigeben' }))
    const rueckfrage = await screen.findByRole('dialog', { name: 'Rechnung freigeben?' })
    await nutzer.click(within(rueckfrage).getByRole('button', { name: 'Freigeben' }))

    expect(
      await screen.findByRole('heading', { name: 'Rechnung RE-2026-0412' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Rechnung RE-2026-0412 ist freigegeben.')).toBeInTheDocument()
    expect(freigabe).toEqual({ version: 0 })
  })

  it('fragt vor dem Verlassen mit ungespeicherten Änderungen nach', async () => {
    const { nutzer, router } = renderAnwendung({ pfad: `/rechnungen/${demoEntwurf().id}` })

    await nutzer.type(await screen.findByLabelText('Beschreibung, Position 1'), ' inkl. Anfahrt')
    await nutzer.click(screen.getByRole('link', { name: 'Abbrechen' }))

    const rueckfrage = await screen.findByRole('dialog', { name: 'Änderungen verwerfen?' })
    await nutzer.click(within(rueckfrage).getByRole('button', { name: 'Weiter bearbeiten' }))
    expect(router.state.location.pathname).toBe(`/rechnungen/${demoEntwurf().id}`)

    await nutzer.click(screen.getByRole('link', { name: 'Abbrechen' }))
    await nutzer.click(
      within(await screen.findByRole('dialog', { name: 'Änderungen verwerfen?' })).getByRole(
        'button',
        { name: 'Verwerfen' },
      ),
    )
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/rechnungen')
    })
  })
})
