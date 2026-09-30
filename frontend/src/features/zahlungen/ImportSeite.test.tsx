import { screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer } from '@/mocks/demo-daten'
import { DEMO_DATEI } from '@/mocks/zahlung-daten'
import type { ZahlungsdateiDto } from '@/shared/api/generiert/modell'
import { renderAnwendung } from '@/test/render-anwendung'

const datei = (name: string) =>
  new File(['<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08"/>'], name, {
    type: 'application/xml',
  })

describe('Zahlungsimport', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  // Das fetch von Node kann die FormData von jsdom nicht senden. Der Test fängt den Upload deshalb vor dem
  // Netz ab; die Übertragung als multipart prüft der E2E-Test gegen das echte Backend.
  it('lädt mehrere Dateien nacheinander hoch und meldet je Datei das Ergebnis', async () => {
    const hochgeladen: string[] = []
    const echtesFetch = globalThis.fetch
    vi.stubGlobal('fetch', (eingabe: RequestInfo | URL, init?: RequestInit) => {
      const adresse = eingabe instanceof Request ? eingabe.url : eingabe.toString()
      if (adresse.endsWith('/api/zahlungsdateien') && init?.method === 'POST') {
        const teil = (init.body as FormData).get('datei') as File
        hochgeladen.push(teil.name)
        return Promise.resolve(
          teil.name === 'doppelt.xml'
            ? new Response(
                JSON.stringify({
                  title: 'Datei bereits importiert',
                  status: 409,
                  code: 'DATEI_BEREITS_IMPORTIERT',
                  detail:
                    'Diese Datei wurde bereits am 15.09.2026 um 08:02 importiert (doppelt.xml).',
                }),
                { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
              )
            : Response.json(
                {
                  ...DEMO_DATEI,
                  dateiname: teil.name,
                  anzahlBuchungen: 3,
                  anzahlNeu: 2,
                  anzahlUebersprungen: 1,
                  anzahlIgnoriert: 0,
                  unbearbeitet: 2,
                } satisfies ZahlungsdateiDto,
                { status: 201 },
              ),
        )
      }
      return echtesFetch(eingabe, init)
    })
    const { nutzer } = renderAnwendung({ pfad: '/zahlungen/import' })

    await screen.findByRole('button', { name: 'Dateien wählen' })
    await nutzer.upload(screen.getByTestId('dateifeld'), [
      datei('camt054-september.xml'),
      datei('doppelt.xml'),
    ])

    const hochladen = screen.getByRole('region', { name: 'Dateien hochladen' })
    expect(
      await within(hochladen).findByText('3 Gutschriften: 2 neu, 1 bereits bekannt.'),
    ).toBeInTheDocument()
    expect(within(hochladen).getByRole('alert')).toHaveTextContent(
      'doppelt.xml: Diese Datei wurde bereits am 15.09.2026 um 08:02 importiert',
    )
    expect(hochgeladen).toEqual(['camt054-september.xml', 'doppelt.xml'])
  })

  it('zeigt das Importprotokoll mit dem Stand des Abgleichs', async () => {
    renderAnwendung({ pfad: '/zahlungen/import' })

    const protokoll = await screen.findByRole('region', { name: 'Importprotokoll' })
    const zeile = await within(protokoll).findByRole('row', { name: /camt054-2026-09-15\.xml/ })
    expect(within(zeile).getByText('15.09.2026, 08:02')).toBeInTheDocument()
    expect(within(zeile).getByText('buchhaltung')).toBeInTheDocument()
    expect(within(zeile).getByRole('link', { name: '3' })).toHaveAttribute(
      'href',
      '/zahlungen/klaerung',
    )
    expect(within(zeile).getByText('fertig')).toBeInTheDocument()
    expect(
      within(zeile).getByRole('button', { name: 'Original camt054-2026-09-15.xml herunterladen' }),
    ).toBeInTheDocument()
  })

  it('lässt Leser das Protokoll sehen, aber nichts hochladen', async () => {
    renderAnwendung({
      pfad: '/zahlungen/import',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    expect(await screen.findByRole('region', { name: 'Importprotokoll' })).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Dateien wählen' })).toBeNull()
    })
  })
})
