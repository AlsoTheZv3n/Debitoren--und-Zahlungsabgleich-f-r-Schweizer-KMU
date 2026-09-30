import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { getUebersichtLesenMockHandler } from '@/shared/api/generiert/endpunkte/reporting/reporting.msw'
import { DEMO_UEBERSICHT } from '@/mocks/reporting-daten'
import { renderAnwendung } from '@/test/render-anwendung'

const kachel = (titel: string) =>
  within(screen.getByRole('region', { name: 'Kennzahlen' }))
    .getAllByRole('listitem')
    .find((element) => element.textContent.includes(titel))

describe('Dashboard', () => {
  it('zeigt die Kennzahlen und führt zu den Listen dahinter', async () => {
    renderAnwendung()

    const kennzahlen = await screen.findByRole('region', { name: 'Kennzahlen' })
    expect(within(kennzahlen).getByText('CHF 184’250.40')).toBeInTheDocument()
    expect(within(kennzahlen).getByText('dazu EUR 6’120.00')).toBeInTheDocument()
    expect(within(kennzahlen).getByRole('link', { name: /Überfällig/ })).toHaveAttribute(
      'href',
      '/rechnungen?ueberfaellig=ja',
    )
    expect(within(kennzahlen).getByRole('link', { name: /Überfällig/ })).toHaveTextContent(
      '57 Rechnungen',
    )
    expect(kachel('Klärungsliste')).toHaveTextContent('4')
    expect(kachel('Mahnvorschläge')).toHaveTextContent('3')
    // 111 von 120 Zahlungen
    expect(kachel('Automatisch zugeordnet')).toHaveTextContent('93 %')

    const importe = screen.getByRole('region', { name: 'Letzte Importe' })
    expect(within(importe).getByText('camt054-2026-09-15.xml')).toBeInTheDocument()
    expect(within(importe).getByText('3 in Klärung')).toBeInTheDocument()
  })

  it('hat zu jedem Diagramm eine Tabelle mit denselben Werten', async () => {
    renderAnwendung()

    const alter = await screen.findByRole('region', { name: 'Altersstruktur der offenen Posten' })
    expect(await within(alter).findByText('31–60 Tage')).toBeInTheDocument()
    expect(within(alter).getByText('CHF 9’820.05')).toBeInTheDocument()
    expect(within(alter).getByText('Dazu offen in EUR:', { exact: false })).toBeInTheDocument()

    const eingaenge = await screen.findByRole('region', { name: 'Zahlungseingänge' })
    expect(within(eingaenge).getByText('Automatisch zugeordnet')).toBeInTheDocument()
    expect(await within(eingaenge).findByText('CHF 38’420.50')).toBeInTheDocument()
  })

  it('zählt ohne Zahlungen keinen Anteil', async () => {
    renderAnwendung({
      handler: [
        getUebersichtLesenMockHandler({
          ...DEMO_UEBERSICHT,
          offen: [],
          ueberfaellig: [],
          inKlaerung: [],
          zahlungen30Tage: 0,
          automatisch30Tage: 0,
          letzteImporte: [],
          letzterMahnlauf: undefined,
        }),
      ],
    })

    const kennzahlen = await screen.findByRole('region', { name: 'Kennzahlen' })
    expect(within(kennzahlen).getAllByText('CHF 0.00')).toHaveLength(2)
    expect(kachel('Automatisch zugeordnet')).toHaveTextContent('–')
    expect(kachel('Mahnvorschläge')).toHaveTextContent('Noch kein Mahnlauf')
    expect(screen.getByText('Noch keine Bankdatei importiert.')).toBeInTheDocument()
  })
})
