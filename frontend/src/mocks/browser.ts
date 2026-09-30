import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

/** Startet MSW im Browser; das Worker-Skript liefert das Vite-Plugin im Modus `mock`. */
export async function starteMocks(): Promise<void> {
  await setupWorker(...handlers).start({ onUnhandledFrame: 'bypass', quiet: true })
}
