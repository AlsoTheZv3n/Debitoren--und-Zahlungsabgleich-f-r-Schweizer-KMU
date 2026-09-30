/**
 * Zahlen aus Eingabefeldern. Akzeptiert Schweizer Schreibweisen wie «1’250.50», «1'250.50» oder «12,5»;
 * gerechnet wird damit nicht, nur geprüft und ans Backend übergeben.
 */
export function alsZahl(text: string): number | null {
  const bereinigt = text
    .trim()
    .replace(/[’'\s]/g, '')
    .replace(',', '.')
  if (!/^\d+(\.\d+)?$/.test(bereinigt)) {
    return null
  }
  return Number(bereinigt)
}

/** Anzahl Nachkommastellen einer Eingabe, etwa 2 für «7.35». */
export function nachkommastellen(text: string): number {
  const bereinigt = text
    .trim()
    .replace(/[’'\s]/g, '')
    .replace(',', '.')
  const punkt = bereinigt.indexOf('.')
  return punkt < 0 ? 0 : bereinigt.length - punkt - 1
}

/** Zahl für ein Eingabefeld, ohne Tausendertrennzeichen: 1250.5 → «1250.50» bei zwei Stellen. */
export function alsEingabe(wert: number, stellen?: number): string {
  return stellen === undefined ? String(wert) : wert.toFixed(stellen)
}
