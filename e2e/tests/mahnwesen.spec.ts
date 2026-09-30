import { anmelden, barrierefrei, expect, test } from './fixtures'
import type { Page } from '@playwright/test'

/** Phase 4: überfällige Rechnung → Mahnlauf → Freigabe → Versand per E-Mail, durch alle Schichten bis Mailpit. */

const MAILPIT = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025'

/** Datum vor so vielen Tagen als yyyy-MM-dd, wie es das Datumsfeld erwartet. */
function vorTagen(tage: number): string {
  const datum = new Date()
  datum.setDate(datum.getDate() - tage)
  return datum.toISOString().slice(0, 10)
}

/**
 * Legt einen Kunden mit E-Mail-Adresse an und gibt ihm eine Rechnung, die seit 15 Tagen überfällig ist: Datum vor
 * 45 Tagen, Frist 30 Tage. Die Zahlungserinnerung folgt nach 10 Tagen.
 */
async function ueberfaelligeRechnung(page: Page, kunde: string, email: string) {
  await page.goto('/kunden/neu')
  const neu = page.getByRole('dialog', { name: 'Neuer Kunde' })
  await neu.getByLabel('Name', { exact: true }).fill(kunde)
  await neu.getByLabel('PLZ').fill('3011')
  await neu.getByLabel('Ort').fill('Bern')
  await neu.getByLabel('E-Mail').fill(email)
  await neu.getByRole('button', { name: 'Kunde anlegen' }).click()
  await page
    .getByRole('dialog', { name: kunde })
    .getByRole('link', { name: 'Neue Rechnung' })
    .click()

  await expect(page.getByRole('combobox', { name: 'Kunde' })).toHaveValue(new RegExp(kunde))
  await page.getByLabel('Rechnungsdatum').fill(vorTagen(45))
  await page.getByLabel('Zahlungsfrist in Tagen').fill('30')
  await page.getByLabel('Beschreibung, Position 1').fill('Jahreswartung Lüftung')
  await page.getByLabel('Einzelpreis, Position 1').fill('480.00')
  await page.getByLabel('MWST, Position 1').selectOption('OHNE')
  await page.getByRole('button', { name: 'Freigeben' }).click()
  await page
    .getByRole('dialog', { name: 'Rechnung freigeben?' })
    .getByRole('button', { name: 'Freigeben' })
    .click()

  const titel = page.getByRole('heading', { name: /^Rechnung RE-\d{4}-\d{4,}$/ })
  await expect(titel).toBeVisible()
  await expect(page.getByText('Überfällig', { exact: true })).toBeVisible()
  return {
    nummer: ((await titel.textContent()) ?? '').replace('Rechnung ', ''),
    pfad: new URL(page.url()).pathname,
  }
}

interface MailpitSuche {
  messages: { Subject: string; Attachments: number; To: { Address: string }[] }[]
}

test('überfällige Rechnung → Mahnlauf → Freigabe → Versand per E-Mail', async ({
  page,
  request,
}) => {
  const kennung = Date.now()
  const kunde = `E2E Mahnkunde ${String(kennung)}`
  const email = `mahnung-${String(kennung)}@e2e.example`
  await anmelden(page, 'admin')
  const rechnung = await ueberfaelligeRechnung(page, kunde, email)

  await page.goto('/mahnwesen')
  await page.getByRole('button', { name: 'Mahnlauf starten' }).click()
  await expect(page.getByText(/^Mahnlauf fertig: \d+ überfällige Rechnungen geprüft/)).toBeVisible()

  await page.getByRole('searchbox', { name: 'Mahnungen suchen' }).fill(kunde)
  const vorschlaege = page.getByRole('region', { name: 'Mahnungen: Vorschläge' })
  const zeile = vorschlaege.getByRole('row', { name: new RegExp(rechnung.nummer) })
  await expect(zeile.getByText('Zahlungserinnerung')).toBeVisible()
  // 15 Tage, je nach Zeitzone des Testrechners um Mitternacht einer mehr oder weniger.
  await expect(zeile.getByText(/^1[4-6] Tage$/)).toBeVisible()
  await zeile
    .getByRole('checkbox', { name: `Zahlungserinnerung zu ${rechnung.nummer} auswählen` })
    .check()
  await page
    .getByRole('region', { name: 'Sammelaktionen' })
    .getByRole('button', { name: 'Freigeben' })
    .click()
  await page
    .getByRole('dialog', { name: 'Einen Vorschlag freigeben?' })
    .getByRole('button', { name: 'Freigeben' })
    .click()
  await expect(page.getByText(/^Eine Mahnung ist freigegeben/)).toBeVisible()

  // Der Versand läuft nach dem Commit im Hintergrund; die Liste zeigt ihn nach dem nächsten Laden.
  await expect(async () => {
    await page.goto(`/mahnwesen?ansicht=versendet&suche=${encodeURIComponent(kunde)}`)
    await expect(
      page
        .getByRole('region', { name: 'Mahnungen: Versendet' })
        .getByRole('row', { name: new RegExp(rechnung.nummer) })
        .getByText('Per E-Mail versendet'),
    ).toBeVisible({ timeout: 2_000 })
  }).toPass({ timeout: 30_000 })

  const suche = await request.get(`${MAILPIT}/api/v1/search`, {
    params: { query: `to:"${email}"` },
  })
  expect(suche.ok()).toBe(true)
  const gefunden = (await suche.json()) as MailpitSuche
  expect(gefunden.messages).toHaveLength(1)
  expect(gefunden.messages[0]?.Subject).toBe(`Zahlungserinnerung zu Rechnung ${rechnung.nummer}`)
  expect(gefunden.messages[0]?.Attachments).toBe(1)

  await page.goto(rechnung.pfad)
  const mahnungen = page.getByRole('region', { name: 'Mahnungen' })
  await expect(mahnungen.getByText('Zahlungserinnerung')).toBeVisible()
  await expect(mahnungen.getByText('Per E-Mail versendet')).toBeVisible()
  const verlauf = page.getByRole('region', { name: 'Verlauf' })
  await expect(verlauf.getByText('Mahnung angelegt')).toBeVisible()
  await expect(verlauf.getByText('Rechnung angelegt')).toBeVisible()
})

test('Dashboard, Mahnwesen und Einstellungen erfüllen WCAG 2.2 AA', async ({ page }) => {
  await anmelden(page, 'admin')
  await expect(page.getByRole('region', { name: 'Kennzahlen' })).toBeVisible()
  // Die Diagramme laden nach; ihre Tabellen erscheinen mit ihnen.
  await expect(page.getByText('Werte als Tabelle')).toHaveCount(2)
  await barrierefrei(page)

  await page.goto('/mahnwesen')
  await expect(page.getByRole('region', { name: 'Mahnungen: Vorschläge' })).toBeVisible()
  await barrierefrei(page)

  await page.goto('/einstellungen/mahnstufen')
  await expect(page.getByRole('region', { name: 'Stufe 1' })).toBeVisible()
  await barrierefrei(page)

  await page.goto('/einstellungen/audit')
  await expect(page.getByRole('region', { name: 'Audit-Log' })).toBeVisible()
  await barrierefrei(page)
})

test('Buchhaltung gibt frei, startet aber keinen Mahnlauf und ändert keine Stufen', async ({
  page,
}) => {
  await anmelden(page, 'buchhaltung', '/mahnwesen')
  await expect(page.getByRole('region', { name: 'Mahnungen: Vorschläge' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Mahnlauf starten' })).toHaveCount(0)

  await page.goto('/einstellungen/mahnstufen')
  await expect(page.getByRole('heading', { name: 'Kein Zugriff' })).toBeVisible()
})
