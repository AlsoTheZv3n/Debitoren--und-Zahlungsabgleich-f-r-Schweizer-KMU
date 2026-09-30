import { defineConfig, devices } from '@playwright/test'

/**
 * Hauptflüsse durch alle Schichten: Browser, Keycloak, Frontend-Container, Backend und PostgreSQL.
 * Standardziel ist der Compose-Stack (`docker compose --profile app up`); mit E2E_BASE_URL lässt
 * sich auch der Vite-Server (http://localhost:5180) prüfen.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8088',
    locale: 'de-CH',
    timezoneId: 'Europe/Zurich',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
