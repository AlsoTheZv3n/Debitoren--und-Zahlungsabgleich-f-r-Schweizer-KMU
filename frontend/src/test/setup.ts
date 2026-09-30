import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import '@/shared/lib/zod-deutsch'
import { setzeMandant, setzeTokenQuelle } from '@/shared/lib/http'
import { server } from './server'

// Die erste Darstellung einer Seite braucht auf langsamen Rechnern mehr als die üblichen 1000 ms.
configure({ asyncUtilTimeout: 5000 })

// jsdom kennt einige Browser-APIs nicht, die Radix für Menüs braucht.
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = class {
    observe() {
      // In jsdom ändert sich keine Grösse.
    }
    unobserve() {
      // Nichts zu beobachten.
    }
    disconnect() {
      // Nichts zu trennen.
    }
  }
}
const elementErsatz: Record<string, () => unknown> = {
  scrollIntoView: () => undefined,
  hasPointerCapture: () => false,
  releasePointerCapture: () => undefined,
}
for (const [name, ersatz] of Object.entries(elementErsatz)) {
  if (!(name in Element.prototype)) {
    Object.defineProperty(Element.prototype, name, { value: ersatz, configurable: true })
  }
}

// jsdom kennt das dialog-Element, aber nicht seine modale Anzeige.
if (!('showModal' in HTMLDialogElement.prototype)) {
  Object.defineProperties(HTMLDialogElement.prototype, {
    showModal: {
      configurable: true,
      value(this: HTMLDialogElement) {
        this.open = true
      },
    },
    close: {
      configurable: true,
      value(this: HTMLDialogElement) {
        if (this.open) {
          this.open = false
          this.dispatchEvent(new Event('close'))
        }
      },
    },
  })
}

beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' })
})

afterEach(() => {
  cleanup()
  server.resetHandlers()
  window.localStorage.clear()
  setzeMandant(null)
  setzeTokenQuelle(() => Promise.resolve(null))
})

afterAll(() => {
  server.close()
})
