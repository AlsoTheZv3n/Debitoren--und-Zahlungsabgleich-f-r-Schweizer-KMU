import { createContext, useContext } from 'react'
import type { MandantDto } from '@/shared/api/generiert/modell'

export interface MandantAuswahl {
  mandanten: MandantDto[]
  aktiv: MandantDto
  waehlen: (id: string) => void
}

export const MandantKontext = createContext<MandantAuswahl | null>(null)

/** Der gewählte Mandant; nur innerhalb des angemeldeten Bereichs verfügbar. */
export function useMandant(): MandantAuswahl {
  const auswahl = useContext(MandantKontext)
  if (!auswahl) {
    throw new Error('useMandant braucht den MandantProvider')
  }
  return auswahl
}

const SPEICHERSCHLUESSEL = 'debitoren.mandant'

/** Zuletzt gewählter Mandant; nur eine Bequemlichkeit, das Backend prüft jede Anfrage. */
export const mandantSpeicher = {
  lesen(): string | null {
    try {
      return window.localStorage.getItem(SPEICHERSCHLUESSEL)
    } catch {
      return null
    }
  },
  schreiben(id: string): void {
    try {
      window.localStorage.setItem(SPEICHERSCHLUESSEL, id)
    } catch {
      // Ohne Speicher gilt beim nächsten Start einfach der erste Mandant.
    }
  },
}
