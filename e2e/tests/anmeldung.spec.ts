import AxeBuilder from '@axe-core/playwright'
import { anmelden, BENUTZER, expect, test } from './fixtures'

/** Gate 1: Login mit Rollen klappt, durch Keycloak, Frontend, Backend und Datenbank. */

test('Admin sieht alle Rollen, beide Mandanten und die Einstellungen', async ({ page }) => {
  await anmelden(page, 'admin')

  const sitzung = page.getByRole('region', { name: 'Ihre Sitzung' })
  await expect(sitzung.getByText(BENUTZER.admin.name)).toBeVisible()
  for (const rolle of ['Admin', 'Buchhaltung', 'Leser']) {
    await expect(sitzung.getByText(rolle, { exact: true })).toBeVisible()
  }
  await expect(sitzung.getByText('Berner Werkstatt AG')).toBeVisible()

  await page.getByRole('button', { name: /Mandant wählen/ }).click()
  await page.getByRole('menuitemradio', { name: /Seeland Gartenbau/ }).click()
  await expect(sitzung.getByText('Seeland Gartenbau GmbH')).toBeVisible()
  await expect(sitzung.getByText('SCOR-Referenz')).toBeVisible()

  await page
    .getByRole('navigation', { name: 'Hauptnavigation' })
    .getByRole('link', { name: 'Einstellungen' })
    .click()
  await expect(page.getByRole('heading', { name: 'Einstellungen' })).toBeVisible()
})

test('Buchhaltung ist auch Leser, aber nicht Admin', async ({ page }) => {
  await anmelden(page, 'buchhaltung')

  const sitzung = page.getByRole('region', { name: 'Ihre Sitzung' })
  await expect(sitzung.getByText(BENUTZER.buchhaltung.name)).toBeVisible()
  await expect(sitzung.getByText('Buchhaltung', { exact: true })).toBeVisible()
  await expect(sitzung.getByText('Leser', { exact: true })).toBeVisible()
  await expect(sitzung.getByText('Admin', { exact: true })).toHaveCount(0)
  await expect(
    page
      .getByRole('navigation', { name: 'Hauptnavigation' })
      .getByRole('link', { name: 'Einstellungen' }),
  ).toHaveCount(0)
})

test('Leser hat einen Mandanten und keinen Zugang zu den Einstellungen', async ({ page }) => {
  await anmelden(page, 'leser')

  await expect(page.getByRole('button', { name: /Mandant wählen/ })).toHaveCount(0)
  await expect(page.getByRole('banner').getByText('Berner Werkstatt AG')).toBeVisible()

  await page.goto('/einstellungen')
  await expect(page.getByRole('heading', { name: 'Kein Zugriff' })).toBeVisible()
})

test('nach der Anmeldung geht es zur ursprünglich aufgerufenen Seite', async ({ page }) => {
  await anmelden(page, 'buchhaltung', '/zahlungen/klaerung')

  await expect(page).toHaveURL(/\/zahlungen\/klaerung$/)
  await expect(page.getByRole('heading', { name: 'Klärungsliste' })).toBeVisible()
})

test('Abmelden beendet die Sitzung bei Keycloak', async ({ page }) => {
  await anmelden(page, 'leser')

  await page.getByRole('button', { name: /Benutzermenü/ }).click()
  await page.getByRole('menuitem', { name: 'Abmelden' }).click()
  await expect(page.locator('#kc-login')).toBeVisible()

  await page.goto('/')
  await expect(page.locator('#kc-login')).toBeVisible()
})

test('die Übersicht erfüllt WCAG 2.2 AA', async ({ page }) => {
  await anmelden(page, 'admin')
  await expect(page.getByRole('region', { name: 'Ihre Sitzung' })).toBeVisible()

  const ergebnis = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze()
  expect(ergebnis.violations).toEqual([])
})
