import type { ZahlungDtoKlaerungsgrund, ZuordnungDtoArt } from '@/shared/api/generiert/modell'

/** Warum eine Zahlung in der Klärungsliste steht, so wie Buchhalterinnen und Buchhalter es sagen. */
export const KLAERUNGSGRUND: Record<ZahlungDtoKlaerungsgrund, string> = {
  OHNE_REFERENZ: 'Ohne Referenz',
  REFERENZ_UNBEKANNT: 'Referenz unbekannt',
  UEBERZAHLUNG: 'Überzahlung',
  WAEHRUNG_ABWEICHEND: 'Andere Währung als die Rechnung',
  RECHNUNG_STORNIERT: 'Rechnung storniert',
  RECHNUNG_BEREITS_BEZAHLT: 'Rechnung bereits bezahlt',
  RESTBETRAG: 'Restbetrag',
  ZUORDNUNG_AUFGEHOBEN: 'Zuordnung aufgehoben',
}

export const ZUORDNUNGSART: Record<ZuordnungDtoArt, string> = {
  AUTO: 'automatisch',
  MANUELL: 'von Hand',
}
