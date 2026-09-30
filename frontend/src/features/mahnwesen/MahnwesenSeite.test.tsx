import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer } from '@/mocks/demo-daten'
import { DEMO_MAHNLAUF, DEMO_VORSCHLAEGE, demoMahnung } from '@/mocks/mahnwesen-daten'
import type { FreigabeErgebnisDto, MahnlaufDto, MahnungDto } from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

const [KELLER_VORSCHLAG, GASSER_VORSCHLAG] = DEMO_VORSCHLAEGE

describe('Mahnwesen', () => {
  it('gibt gewählte Vorschläge gesammelt frei und nennt, was nicht ging', async () => {
    let anfrage: unknown
    const { nutzer } = renderAnwendung({
      pfad: '/mahnwesen',
      handler: [
        http.post('*/api/mahnungen/freigabe', async ({ request }) => {
          anfrage = await request.json()
          return HttpResponse.json<FreigabeErgebnisDto>({
            freigegeben: 1,
            nichtFreigegeben: [
              {
                mahnungId: GASSER_VORSCHLAG?.id ?? '',
                rechnungsnummer: 'RE-2026-0388',
                grund: 'Rechnung inzwischen bezahlt; Vorschlag verworfen.',
              },
            ],
          })
        }),
      ],
    })

    const liste = await screen.findByRole('region', { name: 'Mahnungen: Vorschläge' })
    expect(await within(liste).findByText('Keller Haustechnik AG')).toBeInTheDocument()
    expect(
      screen.getByText(/Letzter Mahnlauf .* \(automatisch\): 14 überfällige/),
    ).toBeInTheDocument()

    await nutzer.click(
      within(liste).getByRole('checkbox', { name: '1. Mahnung zu RE-2026-0412 auswählen' }),
    )
    await nutzer.click(
      within(liste).getByRole('checkbox', { name: '2. Mahnung zu RE-2026-0388 auswählen' }),
    )
    const aktionen = screen.getByRole('region', { name: 'Sammelaktionen' })
    expect(within(aktionen).getByText('2 Vorschläge gewählt')).toBeInTheDocument()
    await nutzer.click(within(aktionen).getByRole('button', { name: 'Freigeben' }))

    const dialog = await screen.findByRole('dialog', { name: '2 Vorschläge freigeben?' })
    expect(within(dialog).getByText(/Eine Mahnung enthält eine Gebühr/)).toBeInTheDocument()
    await nutzer.click(within(dialog).getByRole('button', { name: 'Freigeben' }))

    expect(await screen.findByText(/Eine Mahnung ist freigegeben/)).toBeInTheDocument()
    expect(
      screen.getByText('RE-2026-0388: Rechnung inzwischen bezahlt; Vorschlag verworfen.'),
    ).toBeInTheDocument()
    expect(anfrage).toEqual({
      mahnungen: [
        { id: KELLER_VORSCHLAG?.id, version: 0 },
        { id: GASSER_VORSCHLAG?.id, version: 0 },
      ],
    })
  })

  it('verwirft einen Vorschlag mit Grund und Sperre', async () => {
    let anfrage: Record<string, unknown> = {}
    const { nutzer } = renderAnwendung({
      pfad: '/mahnwesen',
      handler: [
        http.post('*/api/mahnungen/:id/verwerfen', async ({ request, params }) => {
          anfrage = { id: params.id, ...((await request.json()) as object) }
          return HttpResponse.json<MahnungDto>(demoMahnung({ status: 'VERWORFEN' }))
        }),
      ],
    })

    const liste = await screen.findByRole('region', { name: 'Mahnungen: Vorschläge' })
    const zeile = (await within(liste).findByText('Keller Haustechnik AG')).closest('tr')
    if (!zeile) {
      throw new Error('Zeile fehlt')
    }
    await nutzer.click(within(zeile).getByRole('button', { name: 'Verwerfen' }))

    const dialog = await screen.findByRole('dialog', {
      name: '1. Mahnung zu RE-2026-0412 verwerfen?',
    })
    await nutzer.type(within(dialog).getByLabelText(/Grund/), 'Zahlung zugesagt')
    await nutzer.click(within(dialog).getByRole('button', { name: 'Verwerfen' }))

    expect(await screen.findByText('RE-2026-0412: Vorschlag verworfen.')).toBeInTheDocument()
    expect(anfrage).toMatchObject({
      id: KELLER_VORSCHLAG?.id,
      version: 0,
      grund: 'Zahlung zugesagt',
    })
    expect(anfrage.gesperrtBis).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('zeigt Schreiben zum Drucken und markiert sie als versendet', async () => {
    let versendet = false
    const { nutzer } = renderAnwendung({
      pfad: '/mahnwesen?ansicht=versand',
      handler: [
        http.post('*/api/mahnungen/:id/postversand', () => {
          versendet = true
          return HttpResponse.json<MahnungDto>(demoMahnung({ status: 'VERSENDET' }))
        }),
      ],
    })

    const liste = await screen.findByRole('region', { name: 'Mahnungen: Zum Versand' })
    expect(await within(liste).findByText('Zum Drucken')).toBeInTheDocument()
    await nutzer.click(within(liste).getByRole('button', { name: 'Versendet' }))

    expect(
      await screen.findByText('RE-2026-0377: als per Post versendet markiert.'),
    ).toBeInTheDocument()
    expect(versendet).toBe(true)
  })

  it('lässt Admins den Mahnlauf von Hand starten', async () => {
    const { nutzer } = renderAnwendung({
      pfad: '/mahnwesen',
      handler: [
        http.post('*/api/mahnlaeufe', () =>
          HttpResponse.json<MahnlaufDto>(
            { ...DEMO_MAHNLAUF, ausgeloestVon: 'admin', vorschlaege: 1 },
            { status: 201 },
          ),
        ),
      ],
    })

    await nutzer.click(await screen.findByRole('button', { name: 'Mahnlauf starten' }))
    expect(
      await screen.findByText(
        'Mahnlauf fertig: 14 überfällige Rechnungen geprüft, ein neuer Vorschlag.',
      ),
    ).toBeInTheDocument()
  })

  it('Leser sehen Vorschläge, können aber nichts freigeben', async () => {
    renderAnwendung({
      pfad: '/mahnwesen',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    const liste = await screen.findByRole('region', { name: 'Mahnungen: Vorschläge' })
    expect(await within(liste).findByText('Keller Haustechnik AG')).toBeInTheDocument()
    await waitFor(() => {
      expect(within(liste).queryByRole('checkbox')).toBeNull()
    })
    expect(within(liste).queryByRole('button', { name: 'Verwerfen' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Mahnlauf starten' })).toBeNull()
    expect(
      within(liste).getByRole('button', { name: 'Vorschau zu RE-2026-0412 herunterladen' }),
    ).toBeInTheDocument()
  })
})
