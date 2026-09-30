import type { QueryClient } from '@tanstack/react-query'
import type { Problem, ProblemFehlerItem } from '@/shared/api/generiert/modell'

/**
 * Mutator für die von Orval generierten Hooks: setzt Token und Mandant, wirft bei Fehlern einen
 * {@link ApiFehler} mit dem ProblemDetail des Backends.
 */

export const MANDANT_HEADER = 'X-Mandant-Id'

type TokenQuelle = () => Promise<string | null>

let tokenQuelle: TokenQuelle = () => Promise.resolve(null)
let mandantId: string | null = null

/** Wird einmal beim Start gesetzt; liefert das aktuelle Access-Token. */
export function setzeTokenQuelle(quelle: TokenQuelle): void {
  tokenQuelle = quelle
}

/** Wird von der Mandantenwahl gesetzt; jede folgende Anfrage trägt diesen Mandanten. */
export function setzeMandant(id: string | null): void {
  mandantId = id
}

export function aktuellerMandant(): string | null {
  return mandantId
}

export class ApiFehler extends Error implements Problem {
  readonly status: number
  readonly title: string
  readonly code: string
  readonly detail?: string
  readonly type?: string
  readonly instance?: string
  readonly fehler?: ProblemFehlerItem[]

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiFehler'
    this.status = problem.status
    this.title = problem.title
    this.code = problem.code
    this.detail = problem.detail
    this.type = problem.type
    this.instance = problem.instance
    this.fehler = problem.fehler
  }
}

/** Text für die Oberfläche; unerwartete Fehler zeigen keine technischen Details. */
export function fehlertext(fehler: unknown): string {
  if (fehler instanceof ApiFehler) {
    return fehler.detail ?? fehler.title
  }
  return 'Etwas ist schiefgelaufen. Bitte erneut versuchen.'
}

/**
 * JSON-Antworten werden gelesen, alles andere (PDF, SVG) als Blob geliefert. Ohne Accept-Header wählt
 * das Backend das Format des Endpunkts; Fehler kommen immer als ProblemDetail.
 */
export async function apiFetch<T>(url: string, init: RequestInit = {}): Promise<T> {
  const kopf = new Headers(init.headers)
  const token = await tokenQuelle()
  if (token) {
    kopf.set('Authorization', `Bearer ${token}`)
  }
  if (mandantId) {
    kopf.set(MANDANT_HEADER, mandantId)
  }

  const antwort = await fetch(new URL(url, window.location.origin), { ...init, headers: kopf })
  if (!antwort.ok) {
    throw new ApiFehler(await alsProblem(antwort))
  }
  if (antwort.status === 204) {
    return undefined as T
  }
  if (antwort.headers.get('Content-Type')?.includes('json')) {
    return (await antwort.json()) as T
  }
  return (await antwort.blob()) as T
}

/** Verwirft alle Abfragen, deren Pfad so beginnt, etwa Liste und Details der Rechnungen. */
export function invalidieren(queryClient: QueryClient, pfad: string): Promise<void> {
  return queryClient.invalidateQueries({
    predicate: (abfrage) =>
      typeof abfrage.queryKey[0] === 'string' && abfrage.queryKey[0].startsWith(pfad),
  })
}

async function alsProblem(antwort: Response): Promise<Problem> {
  if (antwort.headers.get('Content-Type')?.includes('json')) {
    try {
      const problem = (await antwort.json()) as Partial<Problem>
      if (problem.code && problem.title) {
        return { ...problem, code: problem.code, title: problem.title, status: antwort.status }
      }
    } catch {
      // Kein gültiges JSON: unten als allgemeiner Fehler behandeln.
    }
  }
  return {
    status: antwort.status,
    title: antwort.statusText || 'Fehler',
    code: antwort.status === 401 ? 'NICHT_ANGEMELDET' : 'UNBEKANNTER_FEHLER',
  }
}
