import type { AltersbandDtoBand } from '@/shared/api/generiert/modell'

/** Bezeichnungen der Altersbänder, in der Reihenfolge der API. */
export const ALTERSBAND: Record<AltersbandDtoBand, string> = {
  NICHT_FAELLIG: 'Nicht fällig',
  TAGE_1_30: '1–30 Tage',
  TAGE_31_60: '31–60 Tage',
  TAGE_61_90: '61–90 Tage',
  UEBER_90: 'Über 90 Tage',
}

const monatsformat = new Intl.DateTimeFormat('de-CH', { month: 'short', timeZone: 'UTC' })

/** Kurzer Monatsname mit Jahr, etwa «Sep. 26», aus «2026-09». */
export function monatKurz(monat: string): string {
  const [jahr, nummer] = monat.split('-').map(Number)
  const datum = new Date(Date.UTC(jahr ?? 2000, (nummer ?? 1) - 1, 1))
  return `${monatsformat.format(datum)} ${String(jahr ?? '').slice(2)}`
}

const achsenformat = new Intl.NumberFormat('de-CH', { maximumFractionDigits: 0 })

/** Ganze Zahl mit Schweizer Tausendertrennzeichen für Achsen, etwa «150’000». */
export function achsenwert(wert: number): string {
  return achsenformat.format(wert).replaceAll("'", '’')
}

/**
 * Marken für die Wertachse in runden Schritten, 1, 2 oder 5 mal eine Zehnerpotenz, etwa 0, 10’000, 20’000,
 * 30’000. Die letzte Marke liegt auf oder über dem grössten Wert.
 */
export function achsenmarken(maximum: number, anzahl = 4): number[] {
  if (maximum <= 0) {
    return [0, 1]
  }
  const roh = maximum / anzahl
  const potenz = 10 ** Math.floor(Math.log10(roh))
  const schritt = [1, 2, 5, 10].map((faktor) => faktor * potenz).find((wert) => wert >= roh) ?? roh
  const marken = [0]
  while ((marken[marken.length - 1] ?? 0) < maximum) {
    marken.push((marken[marken.length - 1] ?? 0) + schritt)
  }
  return marken
}
