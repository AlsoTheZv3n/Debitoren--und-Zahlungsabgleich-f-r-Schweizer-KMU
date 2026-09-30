import { keepPreviousData } from '@tanstack/react-query'
import { ListChecks, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useZahlungenSuchen } from '@/shared/api/generiert/endpunkte'
import type { ZahlungDto } from '@/shared/api/generiert/modell'
import { cn } from '@/shared/lib/cn'
import { formatDatum } from '@/shared/lib/format'
import { fehlertext } from '@/shared/lib/http'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { Badge } from '@/shared/ui/Badge'
import { Betrag } from '@/shared/ui/Betrag'
import { Eingabe } from '@/shared/ui/Eingabe'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { ZahlungDetail, type Rueckmeldung } from './ZahlungDetail'
import { KLAERUNGSGRUND } from './zahlungstexte'

/**
 * Der wichtigste Arbeitsbildschirm: links die ungeklärten Zahlungen, rechts die gewählte Zahlung mit
 * Vorschlägen. Die gewählte Zahlung steht in der Adresse; ist sie zugeordnet, rückt die nächste nach.
 */
export function KlaerungSeite() {
  const [parameter, setParameter] = useSearchParams()
  const suche = parameter.get('suche') ?? ''
  const [eingabe, setEingabe] = useState(suche)
  const entprellt = useEntprellt(eingabe)
  const [rueckmeldung, setRueckmeldung] = useState<Rueckmeldung | null>(null)

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
          return neu
        },
        { replace: true },
      )
    }
  }, [entprellt, suche, setParameter])

  const abfrage = useZahlungenSuchen(
    { status: 'KLAERUNG', suche: suche || undefined, groesse: 100 },
    { query: { placeholderData: keepPreviousData } },
  )
  const zahlungen = abfrage.data?.inhalt ?? []
  const gewaehlt =
    zahlungen.find((zahlung) => zahlung.id === parameter.get('zahlung')) ?? zahlungen[0]

  const waehlen = (id: string) => {
    setRueckmeldung(null)
    setParameter(
      (alt) => {
        const neu = new URLSearchParams(alt)
        neu.set('zahlung', id)
        return neu
      },
      { replace: true },
    )
  }

  const anzahl = abfrage.data?.anzahlElemente ?? 0
  return (
    <>
      <Seitenkopf
        titel="Klärungsliste"
        beschreibung={
          abfrage.data
            ? anzahl === 1
              ? 'Eine Zahlung liess sich nicht automatisch zuordnen.'
              : `${anzahl} Zahlungen liessen sich nicht automatisch zuordnen.`
            : 'Zahlungen, die sich nicht automatisch zuordnen liessen.'
        }
      />
      {rueckmeldung && (
        <Meldung art={rueckmeldung.art} className="mb-4">
          {rueckmeldung.text}
        </Meldung>
      )}
      {abfrage.isPending ? (
        <Ladeanzeige />
      ) : abfrage.isError ? (
        <Meldung>{fehlertext(abfrage.error)}</Meldung>
      ) : (
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
          <section
            aria-labelledby="ungeklaert"
            className="rounded-lg border border-slate-200 bg-white"
          >
            <h2 id="ungeklaert" className="sr-only">
              Ungeklärte Zahlungen
            </h2>
            <div className="border-b border-slate-200 p-3">
              <label className="relative block">
                <span className="sr-only">Zahlungen suchen</span>
                <Search
                  aria-hidden
                  className="pointer-events-none absolute top-2.5 left-3 size-4 text-slate-400"
                />
                <Eingabe
                  type="search"
                  value={eingabe}
                  placeholder="Auftraggeber, Referenz oder Mitteilung"
                  className="pl-9"
                  onChange={(ereignis) => {
                    setEingabe(ereignis.target.value)
                  }}
                />
              </label>
            </div>
            {zahlungen.length === 0 ? (
              <div className="p-3">
                <LeererZustand
                  symbol={ListChecks}
                  titel={suche ? 'Keine Treffer' : 'Nichts zu klären'}
                >
                  {suche
                    ? `Keine ungeklärte Zahlung passt zu «${suche}».`
                    : 'Alle importierten Zahlungen sind zugeordnet.'}
                </LeererZustand>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {zahlungen.map((zahlung) => (
                  <li key={zahlung.id}>
                    <ZahlungEintrag
                      zahlung={zahlung}
                      aktiv={zahlung.id === gewaehlt?.id}
                      onWahl={() => {
                        waehlen(zahlung.id)
                      }}
                    />
                  </li>
                ))}
              </ul>
            )}
          </section>
          {gewaehlt && (
            <ZahlungDetail key={gewaehlt.id} zahlung={gewaehlt} onRueckmeldung={setRueckmeldung} />
          )}
        </div>
      )}
    </>
  )
}

function ZahlungEintrag({
  zahlung,
  aktiv,
  onWahl,
}: {
  zahlung: ZahlungDto
  aktiv: boolean
  onWahl: () => void
}) {
  const teilweise = zahlung.rest !== zahlung.betrag
  return (
    <button
      type="button"
      aria-current={aktiv || undefined}
      onClick={onWahl}
      className={cn(
        'flex w-full flex-col gap-1 border-l-2 px-3 py-2.5 text-left text-sm transition-colors hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none focus-visible:ring-inset',
        aktiv ? 'border-brand-600 bg-brand-50/60' : 'border-transparent',
      )}
    >
      <span className="flex items-baseline justify-between gap-3">
        <span className="min-w-0 truncate font-medium text-slate-900">
          {zahlung.auftraggeber ?? 'Unbekannter Auftraggeber'}
        </span>
        <Betrag betrag={zahlung.rest} waehrung={zahlung.waehrung} className="font-medium" />
      </span>
      <span className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
        <span className="tabular-nums">{formatDatum(zahlung.valuta)}</span>
        {zahlung.klaerungsgrund && (
          <Badge variante="klaerung">{KLAERUNGSGRUND[zahlung.klaerungsgrund]}</Badge>
        )}
        {teilweise && (
          <span>
            Rest von <Betrag betrag={zahlung.betrag} waehrung={zahlung.waehrung} />
          </span>
        )}
      </span>
    </button>
  )
}
