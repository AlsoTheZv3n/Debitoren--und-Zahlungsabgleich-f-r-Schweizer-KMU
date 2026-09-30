import { readFile } from 'node:fs/promises'
import { anmelden, barrierefrei, expect, test } from './fixtures'

/** Phase 2: Kunden und Rechnungen durch alle Schichten, bis zum PDF mit QR-Zahlteil. */

test('Buchhaltung legt Kunde und Rechnung an, gibt sie frei, lädt das PDF und storniert', async ({
  page,
}) => {
  const name = `E2E Musterfirma ${Date.now()}`
  await anmelden(page, 'buchhaltung', '/kunden')

  await page.getByRole('link', { name: 'Neuer Kunde' }).click()
  const neu = page.getByRole('dialog', { name: 'Neuer Kunde' })
  await neu.getByLabel('Name', { exact: true }).fill(name)
  await neu.getByLabel('Strasse').fill('Bahnhofstrasse')
  await neu.getByLabel('Nr.').fill('7')
  await neu.getByLabel('PLZ').fill('3011')
  await neu.getByLabel('Ort').fill('Bern')
  await neu.getByRole('button', { name: 'Kunde anlegen' }).click()

  const kunde = page.getByRole('dialog', { name })
  await expect(kunde.getByText('Keine offenen Rechnungen.')).toBeVisible()
  await kunde.getByRole('link', { name: 'Neue Rechnung' }).click()

  await expect(page.getByRole('heading', { name: 'Neue Rechnung' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: 'Kunde' })).toHaveValue(new RegExp(name))
  await page.getByLabel('Beschreibung, Position 1').fill('Beratung Buchhaltung')
  await page.getByLabel('Menge, Position 1').fill('2.5')
  await page.getByLabel('Einheit, Position 1').fill('Std.')
  await page.getByLabel('Einzelpreis, Position 1').fill('180')
  await page.getByRole('button', { name: 'Position hinzufügen' }).click()
  await page.getByLabel('Beschreibung, Position 2').fill('Fachbuch MWST')
  await page.getByLabel('Einzelpreis, Position 2').fill('48.50')
  await page.getByLabel('MWST, Position 2').selectOption('REDUZIERT')

  const summen = page.getByRole('region', { name: 'Summen' })
  await expect(summen.getByText(/CHF\s498\.50/)).toBeVisible()
  await expect(summen.getByText(/MWST 2\.6\s%/)).toBeVisible()
  await expect(page.getByRole('img', { name: 'Vorschau des QR-Zahlteils' })).toBeVisible()
  // Betrag der ersten Position, wie ihn das Backend berechnet: 2.5 × 180.00
  await expect(page.getByRole('table').getByText(/CHF\s450\.00/)).toBeVisible()

  await page.getByRole('button', { name: 'Freigeben' }).click()
  const rueckfrage = page.getByRole('dialog', { name: 'Rechnung freigeben?' })
  await rueckfrage.getByRole('button', { name: 'Freigeben' }).click()

  const titel = page.getByRole('heading', { name: /^Rechnung RE-\d{4}-\d{4,}$/ })
  await expect(titel).toBeVisible()
  const nummer = ((await titel.textContent()) ?? '').replace('Rechnung ', '')
  await expect(page.getByText(`Rechnung ${nummer} ist freigegeben.`)).toBeVisible()
  await expect(page.getByText(/^(QR|SCOR)-Referenz$/)).toBeVisible()
  await expect(
    page.getByRole('img', { name: `QR-Zahlteil der Rechnung ${nummer}` }),
  ).toBeVisible()

  const herunterladen = page.waitForEvent('download')
  await page.getByRole('button', { name: 'PDF herunterladen' }).click()
  const pdf = await herunterladen
  expect(pdf.suggestedFilename()).toBe(`${nummer}.pdf`)
  const inhalt = await readFile(await pdf.path())
  expect(inhalt.subarray(0, 5).toString('latin1')).toBe('%PDF-')
  expect(inhalt.length).toBeGreaterThan(10_000)

  await page.getByRole('button', { name: 'Stornieren' }).click()
  const storno = page.getByRole('dialog', { name: `Rechnung ${nummer} stornieren?` })
  await storno.getByLabel('Grund').fill('E2E-Test')
  await storno.getByRole('button', { name: 'Stornieren' }).click()
  await expect(page.getByText(/^Storniert am \d{2}\.\d{2}\.\d{4}: E2E-Test$/)).toBeVisible()
  await expect(page.getByRole('button', { name: 'Stornieren' })).toHaveCount(0)
})

test('Entwurf speichern, in der Liste finden und gesammelt freigeben', async ({ page }) => {
  const name = `E2E Sammelkunde ${Date.now()}`
  await anmelden(page, 'buchhaltung', '/kunden/neu')

  const neu = page.getByRole('dialog', { name: 'Neuer Kunde' })
  await neu.getByLabel('Name', { exact: true }).fill(name)
  await neu.getByLabel('PLZ').fill('2502')
  await neu.getByLabel('Ort').fill('Biel/Bienne')
  await neu.getByRole('button', { name: 'Kunde anlegen' }).click()
  await expect(page.getByRole('dialog', { name })).toBeVisible()

  // Den Kunden im Editor über die Suche wählen, ganz per Tastatur.
  await page.goto('/rechnungen/neu')
  const kunde = page.getByRole('combobox', { name: 'Kunde' })
  await kunde.fill(name)
  await expect(page.getByRole('option', { name: new RegExp(name), selected: true })).toBeVisible()
  await kunde.press('Enter')
  await expect(kunde).toHaveValue(new RegExp(name))
  await page.getByLabel('Beschreibung, Position 1').fill('Gartenpflege')
  await page.getByLabel('Einzelpreis, Position 1').fill('95')
  await page.getByRole('button', { name: 'Entwurf speichern' }).click()
  await expect(page.getByText('Entwurf gespeichert.')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Rechnungsentwurf' })).toBeVisible()

  await page.goto(`/rechnungen?status=ENTWURF&suche=${encodeURIComponent(name)}`)
  const liste = page.getByRole('region', { name: 'Rechnungsliste' })
  await expect(page.getByLabel('Status')).toHaveValue('ENTWURF')
  await expect(liste.getByRole('row')).toHaveCount(2)
  await liste.getByRole('checkbox', { name: `Entwurf an ${name} auswählen` }).check()

  const aktionen = page.getByRole('region', { name: 'Sammelaktionen' })
  await expect(aktionen.getByText('Ein Entwurf gewählt')).toBeVisible()
  await aktionen.getByRole('button', { name: 'Freigeben' }).click()
  await page
    .getByRole('dialog', { name: 'Einen Entwurf freigeben?' })
    .getByRole('button', { name: 'Freigeben' })
    .click()

  await expect(page.getByText('Keine Treffer')).toBeVisible()
  await page.getByLabel('Status').selectOption('OFFEN')
  await expect(liste.getByRole('row').nth(1)).toContainText(/RE-\d{4}-\d{4,}/)
  await expect(liste.getByRole('row').nth(1)).toContainText('Offen')
})

test('Leser sieht Rechnungen und PDF, kann aber nichts ändern', async ({ page }) => {
  await anmelden(page, 'leser', '/rechnungen?status=OFFEN')

  await expect(page.getByRole('link', { name: 'Neue Rechnung' })).toHaveCount(0)
  const liste = page.getByRole('region', { name: 'Rechnungsliste' })
  await expect(liste.getByRole('table').getByRole('checkbox')).toHaveCount(0)
  await liste.getByRole('row').nth(1).getByRole('link').click()

  await expect(page.getByRole('heading', { name: /^Rechnung RE-/ })).toBeVisible()
  await expect(page.getByRole('button', { name: 'PDF herunterladen' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Stornieren' })).toHaveCount(0)
})

test('Kunden, Rechnungsliste und Editor erfüllen WCAG 2.2 AA', async ({ page }) => {
  await anmelden(page, 'buchhaltung', '/rechnungen')
  await expect(page.getByRole('region', { name: 'Rechnungsliste' }).getByRole('table')).toBeVisible()
  await barrierefrei(page)

  await page.goto('/rechnungen/neu')
  await expect(page.getByRole('heading', { name: 'Neue Rechnung' })).toBeVisible()
  await expect(page.getByRole('img', { name: 'Vorschau des QR-Zahlteils' })).toBeVisible()
  await barrierefrei(page)

  await page.goto('/kunden')
  await page.getByRole('region', { name: 'Kundenliste' }).getByRole('link').first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await barrierefrei(page)
})
