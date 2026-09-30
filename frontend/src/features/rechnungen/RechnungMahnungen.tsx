import { Download } from 'lucide-react'
import { useState } from 'react'
import { MAHNUNGSSTATUS, pdfName, versandstand } from '@/features/mahnwesen/mahnwesen-texte'
import { mahnungPdf, useMahnungenDerRechnung } from '@/shared/api/generiert/endpunkte'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { dateiSpeichern } from '@/shared/lib/download'
import { formatDatum } from '@/shared/lib/format'
import { fehlertext } from '@/shared/lib/http'
import { Badge } from '@/shared/ui/Badge'
import { Betrag } from '@/shared/ui/Betrag'
import { IconButton } from '@/shared/ui/Button'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'

/** Mahnungen zu dieser Rechnung, erste Stufe zuerst, mit dem Schreiben zum Herunterladen. */
export function RechnungMahnungen({ rechnung }: { rechnung: RechnungDto }) {
  const mahnungen = useMahnungenDerRechnung(rechnung.id)
  const [fehler, setFehler] = useState<string | null>(null)
  const sichtbar = mahnungen.data?.filter((mahnung) => mahnung.status !== 'VERWORFEN') ?? []

  if (mahnungen.isSuccess && sichtbar.length === 0) {
    return null
  }

  return (
    <section aria-labelledby="mahnungen" className="rounded-lg border border-slate-200 bg-white">
      <h2 id="mahnungen" className="p-4 pb-2 text-sm font-semibold text-slate-900">
        Mahnungen
      </h2>
      {fehler && <Meldung className="mx-4 mb-2">{fehler}</Meldung>}
      {mahnungen.isPending ? (
        <Ladeanzeige className="px-4" />
      ) : mahnungen.isError ? (
        <Meldung className="m-4">{fehlertext(mahnungen.error)}</Meldung>
      ) : (
        <ul className="divide-y divide-slate-100 border-t border-slate-100">
          {sichtbar.map((mahnung) => {
            const stand =
              mahnung.status === 'VORSCHLAG' ? MAHNUNGSSTATUS.VORSCHLAG : versandstand(mahnung)
            return (
              <li
                key={mahnung.id}
                className="flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-2 text-sm"
              >
                <span className="tabular-nums">{formatDatum(mahnung.mahndatum)}</span>
                <span className="min-w-0 flex-1">
                  {mahnung.bezeichnung}
                  <span className="text-slate-500"> · Frist {formatDatum(mahnung.fristBis)}</span>
                </span>
                <Badge variante={stand.variante}>{stand.text}</Badge>
                {mahnung.gebuehr > 0 && (
                  <span className="text-xs text-slate-500">
                    Gebühr <Betrag betrag={mahnung.gebuehr} waehrung={mahnung.waehrung} />
                  </span>
                )}
                <IconButton
                  variante="dezent"
                  groesse="sm"
                  aria-label={`${mahnung.bezeichnung} herunterladen`}
                  onClick={() => {
                    setFehler(null)
                    mahnungPdf(mahnung.id)
                      .then((inhalt) => {
                        dateiSpeichern(inhalt, pdfName(mahnung))
                      })
                      .catch((ausnahme: unknown) => {
                        setFehler(fehlertext(ausnahme))
                      })
                  }}
                >
                  <Download aria-hidden className="size-4" />
                </IconButton>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
