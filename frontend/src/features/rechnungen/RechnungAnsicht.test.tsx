import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { demoRechnung, KELLER } from '@/mocks/rechnung-daten'
import { demoZuordnung } from '@/mocks/zahlung-daten'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

describe('Rechnungsansicht', () => {
  const objektUrlErstellen = vi.fn<(inhalt: Blob) => string>(() => 'blob:rechnung')

  beforeEach(() => {
    // jsdom kennt keine Objekt-URLs; der Download selbst braucht einen echten Browser.
    Object.assign(URL, {
      createObjectURL: objektUrlErstellen,
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('zeigt Empfänger, Referenz, Positionen, Summen und den Zahlteil', async () => {
    renderAnwendung({ pfad: `/rechnungen/${demoRechnung().id}` })

    expect(
      await screen.findByRole('heading', { name: 'Rechnung RE-2026-0412' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: KELLER.name })).toHaveAttribute(
      'href',
      `/kunden/${KELLER.id}`,
    )
    expect(screen.getByText('00 00000 00000 00000 00000 04126')).toBeInTheDocument()
    expect(screen.getByText('Umwälzpumpe Grundfos')).toBeInTheDocument()
    expect(screen.getByText(`6.5 Std.`)).toBeInTheDocument()
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent('CHF 1’840.00')
    expect(
      await screen.findByRole('img', { name: 'QR-Zahlteil der Rechnung RE-2026-0412' }),
    ).toHaveAttribute('src', expect.stringMatching(/^data:image\/svg\+xml/) as string)
  })

  it('lädt das PDF herunter', async () => {
    const dateinamen: string[] = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      dateinamen.push(this.download)
    })
    const { nutzer } = renderAnwendung({
      pfad: `/rechnungen/${demoRechnung().id}`,
      handler: [
        http.get(
          '*/api/rechnungen/:id/pdf',
          () =>
            new HttpResponse(new Blob(['%PDF-1.7'], { type: 'application/pdf' }), {
              headers: { 'Content-Type': 'application/pdf' },
            }),
        ),
      ],
    })

    await nutzer.click(await screen.findByRole('button', { name: 'PDF herunterladen' }))

    await waitFor(() => {
      expect(dateinamen).toEqual(['RE-2026-0412.pdf'])
    })
    expect(objektUrlErstellen).toHaveBeenCalledWith(expect.any(Blob))
  })

  it('storniert eine offene Rechnung erst mit Grund', async () => {
    let aktuell: RechnungDto = demoRechnung()
    let storno: unknown
    const { nutzer } = renderAnwendung({
      pfad: `/rechnungen/${aktuell.id}`,
      handler: [
        http.get('*/api/rechnungen/:id', () => HttpResponse.json(aktuell)),
        http.post('*/api/rechnungen/:id/storno', async ({ request }) => {
          storno = await request.json()
          aktuell = demoRechnung({
            version: 2,
            status: 'STORNIERT',
            stornoGrund: 'Doppelt erfasst',
            storniertAm: '2026-09-30T10:00:00Z',
          })
          return HttpResponse.json(aktuell)
        }),
      ],
    })

    await nutzer.click(await screen.findByRole('button', { name: 'Stornieren' }))
    const dialog = await screen.findByRole('dialog', { name: 'Rechnung RE-2026-0412 stornieren?' })
    await nutzer.click(within(dialog).getByRole('button', { name: 'Stornieren' }))
    expect(within(dialog).getByText('Bitte einen Grund angeben.')).toBeInTheDocument()
    expect(storno).toBeUndefined()

    await nutzer.type(within(dialog).getByLabelText('Grund'), 'Doppelt erfasst')
    await nutzer.click(within(dialog).getByRole('button', { name: 'Stornieren' }))

    expect(await screen.findByText('Storniert am 30.09.2026: Doppelt erfasst')).toBeInTheDocument()
    expect(storno).toEqual({ grund: 'Doppelt erfasst', version: 1 })
    expect(screen.queryByRole('button', { name: 'Stornieren' })).toBeNull()
    expect(screen.queryByRole('complementary', { name: 'QR-Zahlteil' })).toBeNull()
  })

  it('zeigt Zahlungseingänge und den offenen Betrag und hebt eine Zuordnung nach Rückfrage auf', async () => {
    let aufgehoben: string | undefined
    const { nutzer } = renderAnwendung({
      pfad: `/rechnungen/${demoRechnung().id}`,
      handler: [
        http.get('*/api/rechnungen/:id', () =>
          HttpResponse.json(demoRechnung({ status: 'TEILBEZAHLT', offen: 940 })),
        ),
        http.get('*/api/zuordnungen', () => HttpResponse.json([demoZuordnung()])),
        http.delete('*/api/zuordnungen/:id', ({ params }) => {
          aufgehoben = String(params.id)
          return new HttpResponse(null, { status: 204 })
        }),
      ],
    })

    const angaben = await screen.findByRole('region', { name: 'Angaben' })
    expect(within(angaben).getByText('Offen').nextElementSibling).toHaveTextContent('CHF 940.00')
    const eingaenge = screen.getByRole('region', { name: 'Zahlungseingänge' })
    expect(await within(eingaenge).findByText('Keller Haustechnik AG')).toBeInTheDocument()
    expect(within(eingaenge).getByText('14.09.2026')).toBeInTheDocument()
    expect(within(eingaenge).getByText('automatisch')).toBeInTheDocument()
    expect(within(eingaenge).getByText('CHF 900.00')).toBeInTheDocument()

    await nutzer.click(within(eingaenge).getByRole('button', { name: 'Aufheben' }))
    const dialog = await screen.findByRole('dialog', { name: 'Zuordnung aufheben?' })
    await nutzer.click(within(dialog).getByRole('button', { name: 'Aufheben' }))

    await waitFor(() => {
      expect(aufgehoben).toBe(demoZuordnung().id)
    })
  })
})
