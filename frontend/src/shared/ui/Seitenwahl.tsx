import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

/** Blättern in serverseitig paginierten Listen. */
export function Seitenwahl({
  seite,
  anzahlSeiten,
  anzahlElemente,
  onSeite,
}: {
  seite: number
  anzahlSeiten: number
  anzahlElemente: number
  onSeite: (seite: number) => void
}) {
  if (anzahlElemente === 0) {
    return null
  }
  return (
    <nav
      aria-label="Seiten"
      className="flex items-center justify-between gap-4 border-t border-slate-200 px-3 py-2 text-sm text-slate-600"
    >
      <p>
        Seite {seite + 1} von {Math.max(anzahlSeiten, 1)} · {anzahlElemente.toLocaleString('de-CH')}{' '}
        {anzahlElemente === 1 ? 'Eintrag' : 'Einträge'}
      </p>
      <div className="flex gap-1">
        <Button
          variante="sekundaer"
          groesse="sm"
          disabled={seite <= 0}
          onClick={() => {
            onSeite(seite - 1)
          }}
        >
          <ChevronLeft aria-hidden className="size-4" />
          Zurück
        </Button>
        <Button
          variante="sekundaer"
          groesse="sm"
          disabled={seite + 1 >= anzahlSeiten}
          onClick={() => {
            onSeite(seite + 1)
          }}
        >
          Weiter
          <ChevronRight aria-hidden className="size-4" />
        </Button>
      </div>
    </nav>
  )
}
