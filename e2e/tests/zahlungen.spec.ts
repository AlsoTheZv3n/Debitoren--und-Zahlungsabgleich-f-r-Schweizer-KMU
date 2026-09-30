import { camt054 } from './camt'
import { anmelden, barrierefrei, expect, rechnungFreigeben, test } from './fixtures'

/** Phase 3: Bankdateien importieren, automatisch und von Hand zuordnen, durch alle Schichten. */

const xml = (name: string, inhalt: string) => ({
  name,
  mimeType: 'application/xml',
  buffer: Buffer.from(inhalt, 'utf8'),
})

test('Rechnung → camt-Import → bezahlt', async ({ page }) => {
  const kunde = `E2E Zahler ${Date.now()}`
  await anmelden(page, 'buchhaltung')
  const rechnung = await rechnungFreigeben(page, kunde, '345.50')

  await page.goto('/zahlungen/import')
  const dateiname = `camt054-${rechnung.nummer}.xml`
  await page
    .getByTestId('dateifeld')
    .setInputFiles(xml(dateiname, camt054([{ betrag: '345.50', referenz: rechnung.referenz, auftraggeber: kunde }])))

  const hochladen = page.getByRole('region', { name: 'Dateien hochladen' })
  await expect(hochladen.getByText('1 Gutschrift: 1 neu, 0 bereits bekannt.')).toBeVisible()
  const zeile = page
    .getByRole('region', { name: 'Importprotokoll' })
    .getByRole('row', { name: new RegExp(dateiname.replaceAll('.', '\\.')) })
  await expect(zeile.getByText('fertig')).toBeVisible({ timeout: 15_000 })

  await page.goto(rechnung.pfad)
  await expect(page.getByRole('main').getByText('Bezahlt', { exact: true })).toBeVisible()
  const eingaenge = page.getByRole('region', { name: 'Zahlungseingänge' })
  await expect(eingaenge.getByText(kunde)).toBeVisible()
  await expect(eingaenge.getByText('automatisch')).toBeVisible()
  await expect(eingaenge.getByText(/CHF\s345\.50/)).toBeVisible()
})

test.describe('Klärungsliste', () => {
  test.use({ erwarteteKonsolenfehler: [/status of 409/] })

  test('Zahlung ohne Referenz per Vorschlag zuordnen; dieselbe Datei wird abgelehnt', async ({ page }) => {
    const kunde = `E2E Klärung ${Date.now()}`
    await anmelden(page, 'buchhaltung')
    const rechnung = await rechnungFreigeben(page, kunde, '128.40')

    await page.goto('/zahlungen/import')
    const datei = xml(
      `camt054-klaerung-${Date.now()}.xml`,
      camt054([{ betrag: '128.40', auftraggeber: kunde.toUpperCase(), mitteilung: `Rechnung ${rechnung.nummer}` }]),
    )
    const hochladen = page.getByRole('region', { name: 'Dateien hochladen' })
    await page.getByTestId('dateifeld').setInputFiles(datei)
    await expect(hochladen.getByText('1 Gutschrift: 1 neu, 0 bereits bekannt.')).toBeVisible()
    await page.getByTestId('dateifeld').setInputFiles(datei)
    await expect(hochladen.getByRole('alert')).toContainText('Diese Datei wurde bereits am')

    await page.goto(`/zahlungen/klaerung?suche=${encodeURIComponent(kunde)}`)
    const detail = page.getByRole('region', { name: kunde.toUpperCase() })
    await expect(detail.getByText('Ohne Referenz')).toBeVisible({ timeout: 15_000 })
    await expect(detail.getByText(`Rechnung ${rechnung.nummer}`, { exact: true })).toBeVisible()
    await detail
      .getByRole('region', { name: 'Vorschläge' })
      .getByRole('button', { name: `Rechnung ${rechnung.nummer} zuordnen` })
      .click()
    await expect(page.getByText(`der Rechnung ${rechnung.nummer} zugeordnet.`)).toBeVisible()

    await page.goto(rechnung.pfad)
    await expect(page.getByRole('main').getByText('Bezahlt', { exact: true })).toBeVisible()
    await expect(
      page.getByRole('region', { name: 'Zahlungseingänge' }).getByText('von Hand durch buchhaltung'),
    ).toBeVisible()
  })
})

test('Import und Klärungsliste erfüllen WCAG 2.2 AA', async ({ page }) => {
  await anmelden(page, 'buchhaltung', '/zahlungen/import')
  await expect(page.getByRole('region', { name: 'Importprotokoll' }).getByRole('table')).toBeVisible()
  await barrierefrei(page)

  await page.goto('/zahlungen/klaerung')
  await expect(page.getByRole('heading', { name: 'Klärungsliste' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Vorschläge' })).toBeVisible()
  await barrierefrei(page)
})

test('Leser sieht Import und Klärungsliste, kann aber nichts ändern', async ({ page }) => {
  await anmelden(page, 'leser', '/zahlungen/import')
  await expect(page.getByRole('region', { name: 'Importprotokoll' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Dateien wählen' })).toHaveCount(0)

  await page.goto('/zahlungen/klaerung')
  await expect(page.getByRole('region', { name: 'Vorschläge' })).toBeVisible()
  await expect(page.getByRole('button', { name: /zuordnen$/ })).toHaveCount(0)
})
