import { keepPreviousData, useQueryClient } from '@tanstack/react-query'
import { BellRing, Play, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import {
  useMahnlaeufeAuflisten,
  useMahnlaufAusfuehren,
  useMahnungenSuchen,
} from '@/shared/api/generiert/endpunkte'
import type { MahnlaufDto } from '@/shared/api/generiert/modell'
import { cn } from '@/shared/lib/cn'
import { formatZeitpunkt } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { Button } from '@/shared/ui/Button'
import { Eingabe } from '@/shared/ui/Eingabe'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Seitenwahl } from '@/shared/ui/Seitenwahl'
import { MahnungenTabelle } from './MahnungenTabelle'
import { ANSICHTEN, type Ansicht } from './mahnwesen-texte'

const LEER: Record<Ansicht['schluessel'], { titel: string; text: string }> = {
  vorschlaege: {
    titel: 'Keine Mahnvorschläge',
    text: 'Der tägliche Mahnlauf schlägt für überfällige Rechnungen die nächste Stufe vor; versandt wird erst nach der Freigabe.',
  },
  versand: {
    titel: 'Nichts zum Versand',
    text: 'Hier stehen freigegebene Mahnungen, die noch per Post gehen oder deren E-Mail nicht zugestellt werden konnte.',
  },
  versendet: { titel: 'Noch nichts versendet', text: 'Versandte Mahnungen erscheinen hier.' },
  verworfen: { titel: 'Nichts verworfen', text: 'Verworfene Vorschläge erscheinen hier.' },
}

function alsAnsicht(wert: string | null): Ansicht {
  return ANSICHTEN.find((ansicht) => ansicht.schluessel === wert) ?? ANSICHTEN[0]
}

export function MahnwesenSeite() {
  const [parameter, setParameter] = useSearchParams()
  const location = useLocation()
  const { hatRolle } = useRollen()
  const ansicht = alsAnsicht(parameter.get('ansicht'))
  const suche = parameter.get('suche') ?? ''
  const seite = Number(parameter.get('seite') ?? '0')
  const [eingabe, setEingabe] = useState(suche)
  const entprellt = useEntprellt(eingabe)

  useEffect(() => {
    if (entprellt.trim() !== suche) {
      setParameter(
        (alt) => {
          const neu = new URLSearchParams(alt)
          if (entprellt.trim()) {
            neu.set('suche', entprellt.trim())
          } else {
            neu.delete('suche')
          }
          neu.delete('seite')
          return neu
        },
        { replace: true },
      )
    }
  }, [entprellt, suche, setParameter])

  const abfrage = useMahnungenSuchen(
    { status: ansicht.status, suche: suche || undefined, seite, groesse: 50 },
    { query: { placeholderData: keepPreviousData } },
  )
  const vorschlaege = useMahnungenSuchen({ status: 'VORSCHLAG', groesse: 1 })
  const zumVersand = useMahnungenSuchen({ status: 'FREIGEGEBEN', groesse: 1 })
  const zaehler: Partial<Record<Ansicht['schluessel'], number>> = {
    vorschlaege: vorschlaege.data?.anzahlElemente,
    versand: zumVersand.data?.anzahlElemente,
  }

  return (
    <>
      <Seitenkopf
        titel="Mahnwesen"
        beschreibung="Mahnvorschläge prüfen und gesammelt freigeben."
        aktionen={hatRolle('ADMIN') && <MahnlaufStarten />}
      />
      <LetzterMahnlauf />

      <nav aria-label="Ansichten" className="mb-4 flex flex-wrap gap-1 border-b border-slate-200">
        {ANSICHTEN.map((eintrag) => {
          const aktiv = eintrag.schluessel === ansicht.schluessel
          const anzahl = zaehler[eintrag.schluessel]
          return (
            <Link
              key={eintrag.schluessel}
              to={{
                search: new URLSearchParams({
                  ansicht: eintrag.schluessel,
                  ...(suche ? { suche } : {}),
                }).toString(),
              }}
              aria-current={aktiv ? 'page' : undefined}
              className={cn(
                '-mb-px flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none',
                aktiv
                  ? 'border-brand-600 font-medium text-brand-700'
                  : 'border-transparent text-slate-600 hover:text-slate-900',
              )}
            >
              {eintrag.titel}
              {anzahl !== undefined && anzahl > 0 && (
                <span className="rounded-full bg-slate-100 px-1.5 text-xs text-slate-700 tabular-nums">
                  {anzahl}
                </span>
              )}
            </Link>
          )
        })}
      </nav>

      <section
        aria-label={`Mahnungen: ${ansicht.titel}`}
        className="rounded-lg border border-slate-200 bg-white"
      >
        <div role="search" className="border-b border-slate-200 p-3">
          <label className="relative block w-full max-w-xs">
            <span className="sr-only">Mahnungen suchen</span>
            <Search
              aria-hidden
              className="pointer-events-none absolute top-2.5 left-3 size-4 text-slate-400"
            />
            <Eingabe
              type="search"
              value={eingabe}
              placeholder="Rechnung oder Kunde"
              className="pl-9"
              onChange={(ereignis) => {
                setEingabe(ereignis.target.value)
              }}
            />
          </label>
        </div>
        {abfrage.isPending ? (
          <Ladeanzeige className="px-3" />
        ) : abfrage.isError ? (
          <Meldung className="m-3">{fehlertext(abfrage.error)}</Meldung>
        ) : abfrage.data.inhalt.length === 0 ? (
          <LeererZustand
            symbol={BellRing}
            titel={suche ? 'Keine Treffer' : LEER[ansicht.schluessel].titel}
          >
            {suche ? 'Keine Mahnung passt zu dieser Suche.' : LEER[ansicht.schluessel].text}
          </LeererZustand>
        ) : (
          <>
            {/* Eine andere Ansicht, Suche oder Seite beginnt mit leerer Auswahl. */}
            <MahnungenTabelle
              key={location.search}
              ansicht={ansicht}
              mahnungen={abfrage.data.inhalt}
              bearbeitbar={hatRolle('BUCHHALTUNG')}
            />
            <Seitenwahl
              seite={abfrage.data.seite}
              anzahlSeiten={abfrage.data.anzahlSeiten}
              anzahlElemente={abfrage.data.anzahlElemente}
              onSeite={(neueSeite) => {
                setParameter((alt) => {
                  const neu = new URLSearchParams(alt)
                  neu.set('seite', String(neueSeite))
                  return neu
                })
              }}
            />
          </>
        )}
      </section>
    </>
  )
}

function LetzterMahnlauf() {
  const laeufe = useMahnlaeufeAuflisten({ anzahl: 1 })
  const lauf = laeufe.data?.[0]
  if (!lauf) {
    return null
  }
  return (
    <p className="-mt-4 mb-4 text-sm text-slate-500">
      Letzter Mahnlauf {formatZeitpunkt(lauf.ausgefuehrtAm)}
      {lauf.ausgeloestVon === 'system' ? ' (automatisch)' : ` von ${lauf.ausgeloestVon}`}:{' '}
      {laufergebnis(lauf)}
    </p>
  )
}

function laufergebnis(lauf: MahnlaufDto): string {
  const vorschlaege =
    lauf.vorschlaege === 1 ? 'ein neuer Vorschlag' : `${String(lauf.vorschlaege)} neue Vorschläge`
  return `${String(lauf.geprueft)} überfällige Rechnungen geprüft, ${vorschlaege}.`
}

/** Nur für Admins: startet den Lauf mit Stichtag heute, etwa nach einer Änderung der Mahnstufen. */
function MahnlaufStarten() {
  const queryClient = useQueryClient()
  const ausfuehren = useMahnlaufAusfuehren()
  const [meldung, setMeldung] = useState<{ art: 'erfolg' | 'fehler'; text: string } | null>(null)

  const starten = async () => {
    setMeldung(null)
    try {
      const lauf = await ausfuehren.mutateAsync()
      setMeldung({ art: 'erfolg', text: `Mahnlauf fertig: ${laufergebnis(lauf)}` })
    } catch (ausnahme) {
      setMeldung({ art: 'fehler', text: fehlertext(ausnahme) })
    }
    await invalidieren(queryClient, '/api/mahnungen')
    await invalidieren(queryClient, '/api/mahnlaeufe')
    await invalidieren(queryClient, '/api/reporting')
  }

  return (
    <div className="flex flex-col items-end gap-2">
      <Button disabled={ausfuehren.isPending} onClick={() => void starten()}>
        <Play aria-hidden className="size-4" />
        {ausfuehren.isPending ? 'Mahnlauf läuft…' : 'Mahnlauf starten'}
      </Button>
      {meldung && (
        <Meldung art={meldung.art} className="max-w-md">
          {meldung.text}
        </Meldung>
      )}
    </div>
  )
}
