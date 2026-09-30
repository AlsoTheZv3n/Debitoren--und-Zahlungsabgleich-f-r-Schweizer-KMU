import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { DEMO_MAHNSTUFEN, DEMO_MANDANT_DETAIL } from '@/mocks/mahnwesen-daten'
import type {
  MahnstufeDto,
  MahnstufenAnfrageDto,
  MandantDetailDto,
  MwstSatzDto,
} from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

describe('Einstellungen', () => {
  it('öffnet den Mandanten und speichert eine geänderte E-Mail-Adresse', async () => {
    let anfrage: Record<string, unknown> = {}
    const { nutzer } = renderAnwendung({
      pfad: '/einstellungen',
      handler: [
        http.put('*/api/mandanten/aktuell', async ({ request }) => {
          anfrage = (await request.json()) as Record<string, unknown>
          return HttpResponse.json<MandantDetailDto>({
            ...DEMO_MANDANT_DETAIL,
            email: 'debitoren@berner-werkstatt.example',
            version: 4,
          })
        }),
      ],
    })

    expect(await screen.findByRole('heading', { name: 'Einstellungen' })).toBeInTheDocument()
    const formular = await screen.findByRole('region', { name: 'Mandant' })
    expect(within(formular).getByLabelText(/QR-IBAN/)).toHaveValue('CH4431999123000889012')
    const email = within(formular).getByLabelText(/E-Mail/)
    await nutzer.clear(email)
    await nutzer.type(email, 'debitoren@berner-werkstatt.example')
    await nutzer.click(within(formular).getByRole('button', { name: 'Speichern' }))

    expect(
      await screen.findByText('Die Angaben des Mandanten sind gespeichert.'),
    ).toBeInTheDocument()
    expect(anfrage).toMatchObject({
      name: 'Berner Werkstatt AG',
      iban: 'CH5800791123000889012',
      qrIban: 'CH4431999123000889012',
      email: 'debitoren@berner-werkstatt.example',
      rundungFuenfRappen: true,
      version: 3,
    })
  })

  it('prüft die Reihenfolge der Mahnstufen und speichert mit den Versionen je Position', async () => {
    let anfrage: MahnstufenAnfrageDto | undefined
    const { nutzer } = renderAnwendung({
      pfad: '/einstellungen/mahnstufen',
      handler: [
        http.put('*/api/mahnstufen', async ({ request }) => {
          anfrage = (await request.json()) as MahnstufenAnfrageDto
          return HttpResponse.json<MahnstufeDto[]>(
            DEMO_MAHNSTUFEN.map((stufe) => ({ ...stufe, version: stufe.version + 1 })),
          )
        }),
      ],
    })

    const dritte = await screen.findByRole('region', { name: 'Stufe 3' })
    const tage = within(dritte).getByLabelText(/Nach Fälligkeit/)
    await nutzer.clear(tage)
    await nutzer.type(tage, '20')
    await nutzer.click(screen.getByRole('button', { name: 'Mahnstufen speichern' }))
    expect(await within(dritte).findByText(/Muss später sein als Stufe 2/)).toBeInTheDocument()
    expect(anfrage).toBeUndefined()

    await nutzer.clear(tage)
    await nutzer.type(tage, '60')
    const gebuehr = within(dritte).getByLabelText(/Gebühr/)
    await nutzer.clear(gebuehr)
    await nutzer.type(gebuehr, '25')
    await nutzer.click(screen.getByRole('button', { name: 'Mahnstufen speichern' }))

    expect(await screen.findByText(/Die Mahnstufen sind gespeichert/)).toBeInTheDocument()
    expect(anfrage?.stufen).toHaveLength(3)
    expect(anfrage?.stufen[0]).toMatchObject({ version: 0, mindestabstandTage: 0 })
    expect(anfrage?.stufen[2]).toMatchObject({
      version: 0,
      bezeichnung: '2. Mahnung',
      tageNachFaelligkeit: 60,
      gebuehr: 25,
    })
  })

  it('lehnt unbekannte Platzhalter im Brieftext ab', async () => {
    const { nutzer } = renderAnwendung({ pfad: '/einstellungen/mahnstufen' })

    const erste = await screen.findByRole('region', { name: 'Stufe 1' })
    const text = within(erste).getByLabelText(/Brieftext/)
    await nutzer.clear(text)
    await nutzer.type(text, 'Rechnung {{nummer} ist offen.')
    await nutzer.click(screen.getByRole('button', { name: 'Mahnstufen speichern' }))

    expect(await within(erste).findByText(/Unbekannter Platzhalter/)).toBeInTheDocument()
  })

  it('erfasst einen neuen MWST-Satz ab Stichtag', async () => {
    let anfrage: unknown
    const { nutzer } = renderAnwendung({
      pfad: '/einstellungen/mwst',
      handler: [
        http.post('*/api/mwst-saetze', async ({ request }) => {
          anfrage = await request.json()
          return HttpResponse.json<MwstSatzDto>(
            {
              id: 'neu',
              code: 'NORMAL',
              bezeichnung: 'Normalsatz',
              satz: 8.5,
              gueltigAb: '2028-01-01',
            },
            { status: 201 },
          )
        }),
      ],
    })

    const formular = await screen.findByRole('region', { name: 'Neuer Satz ab Stichtag' })
    await nutzer.type(within(formular).getByLabelText('Prozent'), '8.5')
    await nutzer.type(within(formular).getByLabelText('Gültig ab'), '2028-01-01')
    await nutzer.click(within(formular).getByRole('button', { name: 'Satz erfassen' }))

    expect(
      await within(formular).findByText(/Normalsatz 8.5\s%\sgilt ab 01.01.2028/),
    ).toBeInTheDocument()
    expect(anfrage).toEqual({ code: 'NORMAL', satz: 8.5, gueltigAb: '2028-01-01' })
  })

  it('zeigt das Audit-Log mit Link zur Rechnung', async () => {
    renderAnwendung({ pfad: '/einstellungen/audit' })

    const log = await screen.findByRole('region', { name: 'Audit-Log' })
    expect(
      await within(log).findByRole('link', { name: 'RE-2026-0412 · Keller Haustechnik AG' }),
    ).toBeInTheDocument()
    expect(within(log).getByText('Rechnung geändert')).toBeInTheDocument()
    expect(within(log).getByText('Entwurf')).toHaveClass('line-through')
  })
})
