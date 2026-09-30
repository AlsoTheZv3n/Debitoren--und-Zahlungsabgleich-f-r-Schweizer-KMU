import { createContext, useContext } from 'react'

/**
 * Was die Oberfläche über die Anmeldung wissen muss. Komponenten kennen nur diese Schnittstelle; im
 * Betrieb steckt Keycloak dahinter, in Tests und im Mock-Modus eine feste Anmeldung.
 */
export interface Anmeldung {
  status: 'laedt' | 'angemeldet' | 'abgemeldet' | 'fehler'
  fehler?: string
  anmelden: () => void
  abmelden: () => void
}

export const AnmeldungKontext = createContext<Anmeldung | null>(null)

export function useAnmeldung(): Anmeldung {
  const anmeldung = useContext(AnmeldungKontext)
  if (!anmeldung) {
    throw new Error('useAnmeldung braucht einen Anmeldungs-Provider')
  }
  return anmeldung
}

/** Merkt sich beim Start der Anmeldung, wohin der Benutzer wollte. */
export function anmeldeZustand(): { zurueck: string } {
  return { zurueck: window.location.pathname + window.location.search }
}

/**
 * Ziel nach der Rückkehr von Keycloak. Nur Pfade innerhalb der Anwendung sind erlaubt, damit der
 * Parameter nicht auf fremde Seiten umleiten kann.
 */
export function zielNachAnmeldung(zustand: unknown): string {
  const zurueck =
    typeof zustand === 'object' && zustand !== null && 'zurueck' in zustand
      ? zustand.zurueck
      : undefined
  return typeof zurueck === 'string' && zurueck.startsWith('/') && !zurueck.startsWith('//')
    ? zurueck
    : '/'
}
