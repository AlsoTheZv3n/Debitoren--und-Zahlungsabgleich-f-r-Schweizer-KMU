/** Bietet einen Blob, etwa ein Rechnungs-PDF, als Datei zum Speichern an. */
export function dateiSpeichern(inhalt: Blob, dateiname: string): void {
  const adresse = URL.createObjectURL(inhalt)
  const verweis = document.createElement('a')
  verweis.href = adresse
  verweis.download = dateiname
  document.body.append(verweis)
  verweis.click()
  verweis.remove()
  // Nach dem Klick hat der Browser die Datei übernommen.
  setTimeout(() => {
    URL.revokeObjectURL(adresse)
  }, 1000)
}

/** SVG-Text als Bildquelle; die CSP erlaubt data:-Bilder. */
export function svgAlsBildquelle(svg: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
