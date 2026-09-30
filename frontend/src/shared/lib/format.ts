/**
 * Formate für die Schweiz. Beträge und Daten werden nur hier formatiert; gerechnet wird im Backend.
 */

const GESCHUETZTES_LEERZEICHEN = String.fromCodePoint(0xa0)
const APOSTROPH = String.fromCodePoint(0x2019)

const betragsformate = new Map<string, Intl.NumberFormat>()

function betragsformat(waehrung: string): Intl.NumberFormat {
  let format = betragsformate.get(waehrung)
  if (!format) {
    format = new Intl.NumberFormat('de-CH', {
      style: 'currency',
      currency: waehrung,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
    betragsformate.set(waehrung, format)
  }
  return format
}

/**
 * Betrag mit Währung und Schweizer Tausendertrennzeichen, etwa «CHF 1’840.00». Je nach ICU-Version
 * liefern Browser ein gerades oder ein typografisches Apostroph; hier gilt immer das typografische.
 * Zwischen Währung und Betrag steht ein geschütztes Leerzeichen, damit nichts umbricht.
 */
export function formatBetrag(betrag: number, waehrung: string): string {
  return betragsformat(waehrung)
    .format(betrag)
    .replaceAll("'", APOSTROPH)
    .replace(/\s/gu, GESCHUETZTES_LEERZEICHEN)
}

const datumsformat = new Intl.DateTimeFormat('de-CH', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'Europe/Zurich',
})

/**
 * Datum als dd.MM.yyyy. Ein reines Datum (2026-09-12) wird ohne Zeitzonenumrechnung gelesen, damit es
 * nicht auf den Vortag rutscht; Zeitpunkte werden in Schweizer Zeit dargestellt.
 */
export function formatDatum(wert: string): string {
  const reinesDatum = /^(\d{4})-(\d{2})-(\d{2})$/.exec(wert)
  if (reinesDatum) {
    const [, jahr, monat, tag] = reinesDatum
    return `${tag ?? ''}.${monat ?? ''}.${jahr ?? ''}`
  }
  const zeitpunkt = new Date(wert)
  if (Number.isNaN(zeitpunkt.getTime())) {
    throw new RangeError(`Kein gültiges Datum: ${wert}`)
  }
  return datumsformat.format(zeitpunkt)
}

/** Initialen für den Avatar, etwa «AM» für Andrea Meier. */
export function initialen(name: string): string {
  const teile = name.trim().split(/\s+/).filter(Boolean)
  const erste = teile[0]?.[0] ?? ''
  const letzte = teile.length > 1 ? (teile[teile.length - 1]?.[0] ?? '') : ''
  return (erste + letzte).toUpperCase()
}

const mengenformat = new Intl.NumberFormat('de-CH', { maximumFractionDigits: 3 })

/** Menge wie auf der Rechnung, etwa «1’250.5»; höchstens drei Nachkommastellen. */
export function formatMenge(menge: number): string {
  return mengenformat.format(menge).replaceAll("'", APOSTROPH)
}

const prozentformat = new Intl.NumberFormat('de-CH', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 3,
})

/** MWST-Satz wie auf der Rechnung, etwa «8.1 %». */
export function formatProzent(prozent: number): string {
  return `${prozentformat.format(prozent)}${GESCHUETZTES_LEERZEICHEN}%`
}

const isoDatum = new Intl.DateTimeFormat('en-CA', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  timeZone: 'Europe/Zurich',
})

/** Heutiges Datum in Schweizer Zeit als yyyy-MM-dd, passend für Datumsfelder und die API. */
export function heuteIso(jetzt: Date = new Date()): string {
  return isoDatum.format(jetzt)
}

const zeitpunktformat = new Intl.DateTimeFormat('de-CH', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'Europe/Zurich',
})

/** Zeitpunkt in Schweizer Zeit, etwa «30.09.2026, 10:15». */
export function formatZeitpunkt(wert: string): string {
  return zeitpunktformat.format(new Date(wert))
}

/**
 * Zahlungsreferenz in Gruppen, wie sie auf dem Zahlteil steht: die QR-Referenz als 2 + 5 × 5 Ziffern, die
 * SCOR-Referenz in Vierergruppen.
 */
export function formatReferenz(referenz: string): string {
  const ohneLeerzeichen = referenz.replace(/\s/g, '')
  if (/^\d{27}$/.test(ohneLeerzeichen)) {
    return `${ohneLeerzeichen.slice(0, 2)} ${(ohneLeerzeichen.slice(2).match(/\d{5}/g) ?? []).join(' ')}`
  }
  return (ohneLeerzeichen.match(/.{1,4}/g) ?? []).join(' ')
}
