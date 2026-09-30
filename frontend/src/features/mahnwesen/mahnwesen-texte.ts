import type { MahnungDto, MahnungDtoStatus } from '@/shared/api/generiert/modell'
import type { BadgeVariante } from '@/shared/ui/Badge'

/** Ansichten der Mahnwesen-Seite; jede zeigt einen Status. */
export const ANSICHTEN = [
  { schluessel: 'vorschlaege', status: 'VORSCHLAG', titel: 'Vorschläge' },
  { schluessel: 'versand', status: 'FREIGEGEBEN', titel: 'Zum Versand' },
  { schluessel: 'versendet', status: 'VERSENDET', titel: 'Versendet' },
  { schluessel: 'verworfen', status: 'VERWORFEN', titel: 'Verworfen' },
] as const satisfies readonly { schluessel: string; status: MahnungDtoStatus; titel: string }[]

export type Ansicht = (typeof ANSICHTEN)[number]

export const MAHNUNGSSTATUS: Record<MahnungDtoStatus, { text: string; variante: BadgeVariante }> = {
  VORSCHLAG: { text: 'Vorschlag', variante: 'neutral' },
  FREIGEGEBEN: { text: 'Freigegeben', variante: 'brand' },
  VERSENDET: { text: 'Versendet', variante: 'brand' },
  VERWORFEN: { text: 'Verworfen', variante: 'neutral' },
}

/** Wie es mit einer freigegebenen Mahnung weitergeht, in Worten der Buchhaltung. */
export function versandstand(mahnung: MahnungDto): { text: string; variante: BadgeVariante } {
  if (mahnung.status === 'VERSENDET') {
    return {
      text: mahnung.versandweg === 'POST' ? 'Per Post versendet' : 'Per E-Mail versendet',
      variante: 'brand',
    }
  }
  if (mahnung.versandfehler) {
    return { text: 'Versand fehlgeschlagen', variante: 'ueberfaellig' }
  }
  if (mahnung.versandweg === 'POST') {
    return { text: 'Zum Drucken', variante: 'teilbezahlt' }
  }
  return { text: 'E-Mail wird gesendet', variante: 'neutral' }
}

/** Dateiname für das Schreiben, etwa «RE-2026-0412-Stufe-2.pdf». */
export function pdfName(mahnung: MahnungDto): string {
  return `${mahnung.rechnungsnummer}-Stufe-${String(mahnung.stufe)}.pdf`
}
