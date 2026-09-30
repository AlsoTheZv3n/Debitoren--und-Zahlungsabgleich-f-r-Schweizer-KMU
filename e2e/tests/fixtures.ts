import AxeBuilder from '@axe-core/playwright'
import { test as basis, expect, type Page } from '@playwright/test'

export type Testbenutzer = 'admin' | 'buchhaltung' | 'leser'

/** Die drei Benutzer aus dem Keycloak-Realm-Import; Passwort gleich Benutzername. */
export const BENUTZER: Record<Testbenutzer, { name: string }> = {
  admin: { name: 'Andrea Meier' },
  buchhaltung: { name: 'Beat Frei' },
  leser: { name: 'Lea Steiner' },
}

/** Meldet sich über das echte Keycloak-Formular an. */
export async function anmelden(page: Page, benutzer: Testbenutzer, pfad = '/') {
  await page.goto(pfad)
  await expect(page.locator('#kc-login')).toBeVisible()
  await page.locator('#username').fill(benutzer)
  await page.locator('#password').fill(benutzer)
  await page.locator('#kc-login').click()
  await expect(page.getByRole('navigation', { name: 'Hauptnavigation' })).toBeVisible()
}

const WCAG = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

export async function barrierefrei(page: Page) {
  const ergebnis = await new AxeBuilder({ page }).withTags(WCAG).analyze()
  expect(ergebnis.violations).toEqual([])
}

/**
 * Legt einen Kunden an und gibt ihm eine Rechnung ohne MWST, damit das Total genau dem Preis entspricht.
 * Liefert Nummer, Referenz ohne Leerzeichen und den Pfad der Rechnung.
 */
export async function rechnungFreigeben(page: Page, kunde: string, preis: string) {
  await page.goto('/kunden/neu')
  const neu = page.getByRole('dialog', { name: 'Neuer Kunde' })
  await neu.getByLabel('Name', { exact: true }).fill(kunde)
  await neu.getByLabel('PLZ').fill('3011')
  await neu.getByLabel('Ort').fill('Bern')
  await neu.getByRole('button', { name: 'Kunde anlegen' }).click()
  await page.getByRole('dialog', { name: kunde }).getByRole('link', { name: 'Neue Rechnung' }).click()

  await expect(page.getByRole('combobox', { name: 'Kunde' })).toHaveValue(new RegExp(kunde))
  await page.getByLabel('Beschreibung, Position 1').fill('Wartung Heizung')
  await page.getByLabel('Einzelpreis, Position 1').fill(preis)
  await page.getByLabel('MWST, Position 1').selectOption('OHNE')
  await page.getByRole('button', { name: 'Freigeben' }).click()
  await page
    .getByRole('dialog', { name: 'Rechnung freigeben?' })
    .getByRole('button', { name: 'Freigeben' })
    .click()

  const titel = page.getByRole('heading', { name: /^Rechnung RE-\d{4}-\d{4,}$/ })
  await expect(titel).toBeVisible()
  const nummer = ((await titel.textContent()) ?? '').replace('Rechnung ', '')
  const referenz = await page
    .getByText(/^(QR|SCOR)-Referenz$/)
    .locator('xpath=following-sibling::dd[1]')
    .textContent()
  return {
    nummer,
    referenz: (referenz ?? '').replace(/\s/g, ''),
    pfad: new URL(page.url()).pathname,
  }
}

/**
 * Jede Seite sammelt Verstösse gegen die Content Security Policy und Fehler in der Konsole; nach dem
 * Test darf keiner übrig sein. Ein Test, der absichtlich einen Fehler auslöst, etwa einen 409 beim
 * doppelten Import, nennt die erwartete Meldung in {@code erwarteteKonsolenfehler}.
 */
export const test = basis.extend<{ konsole: string[]; erwarteteKonsolenfehler: RegExp[] }>({
  erwarteteKonsolenfehler: [[], { option: true }],
  konsole: [
    async ({ page, erwarteteKonsolenfehler }, use) => {
      const meldungen: string[] = []
      page.on('console', (nachricht) => {
        if (nachricht.type() === 'error') {
          meldungen.push(nachricht.text())
        }
      })
      page.on('pageerror', (fehler) => meldungen.push(fehler.message))
      await use(meldungen)
      const unerwartet = meldungen.filter(
        (meldung) => !erwarteteKonsolenfehler.some((muster) => muster.test(meldung)),
      )
      expect(unerwartet, 'Konsolenfehler oder CSP-Verstösse').toEqual([])
    },
    { auto: true },
  ],
})

export { expect }
