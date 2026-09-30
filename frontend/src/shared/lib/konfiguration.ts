/**
 * Werte, die je Umgebung verschieden sind. Sie kommen zur Laufzeit aus /config.json, damit Staging und
 * Produktion exakt dasselbe Image nutzen. Lokal liefert Vite public/config.json, im Container erzeugt
 * nginx die Datei aus Umgebungsvariablen.
 */
export interface Laufzeitkonfiguration {
  oidc: {
    authority: string
    clientId: string
  }
}

export async function ladeKonfiguration(): Promise<Laufzeitkonfiguration> {
  const antwort = await fetch('/config.json', { cache: 'no-store' })
  if (!antwort.ok) {
    throw new Error(`Konfiguration nicht ladbar (HTTP ${antwort.status})`)
  }
  const konfiguration = (await antwort.json()) as Partial<Laufzeitkonfiguration>
  if (!konfiguration.oidc?.authority || !konfiguration.oidc.clientId) {
    throw new Error('Konfiguration unvollständig: oidc.authority und oidc.clientId fehlen')
  }
  return konfiguration as Laufzeitkonfiguration
}
