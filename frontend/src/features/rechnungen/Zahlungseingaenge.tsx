import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ZUORDNUNGSART } from '@/features/zahlungen/zahlungstexte'
import { useZuordnungAufheben, useZuordnungenDerRechnung } from '@/shared/api/generiert/endpunkte'
import type { RechnungDto, ZuordnungDto } from '@/shared/api/generiert/modell'
import { formatDatum } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { Betrag } from '@/shared/ui/Betrag'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'

/**
 * Zahlungen, die dieser Rechnung zugeordnet sind. Wer eine Zuordnung aufhebt, schickt die Zahlung zurück in
 * die Klärungsliste; die Rechnung ist danach wieder offen oder teilbezahlt.
 */
export function Zahlungseingaenge({ rechnung }: { rechnung: RechnungDto }) {
  const queryClient = useQueryClient()
  const { hatRolle } = useRollen()
  const eingaenge = useZuordnungenDerRechnung({ rechnungId: rechnung.id })
  const aufheben = useZuordnungAufheben()
  const [aufzuheben, setAufzuheben] = useState<ZuordnungDto | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)

  const bestaetigen = async () => {
    if (!aufzuheben) {
      return
    }
    try {
      await aufheben.mutateAsync({ id: aufzuheben.id })
      setFehler(null)
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    }
    setAufzuheben(null)
    await invalidieren(queryClient, '/api/rechnungen')
    await invalidieren(queryClient, '/api/zuordnungen')
    await invalidieren(queryClient, '/api/zahlungen')
    await invalidieren(queryClient, '/api/audit')
  }

  return (
    <section
      aria-labelledby="zahlungseingaenge"
      className="rounded-lg border border-slate-200 bg-white"
    >
      <h2 id="zahlungseingaenge" className="p-4 pb-2 text-sm font-semibold text-slate-900">
        Zahlungseingänge
      </h2>
      {fehler && <Meldung className="mx-4 mb-2">{fehler}</Meldung>}
      {eingaenge.isPending ? (
        <Ladeanzeige className="px-4" />
      ) : eingaenge.isError ? (
        <Meldung className="m-4">{fehlertext(eingaenge.error)}</Meldung>
      ) : eingaenge.data.length === 0 ? (
        <p className="px-4 pb-4 text-sm text-slate-500">Noch keine Zahlung eingegangen.</p>
      ) : (
        <ul className="divide-y divide-slate-100 border-t border-slate-100">
          {eingaenge.data.map((eingang) => (
            <li
              key={eingang.id}
              className="flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-2 text-sm"
            >
              <span className="tabular-nums">
                {eingang.valuta ? formatDatum(eingang.valuta) : '–'}
              </span>
              <span className="min-w-0 flex-1 truncate">
                {eingang.auftraggeber ?? 'Unbekannter Auftraggeber'}
              </span>
              <span className="text-xs text-slate-500">
                {ZUORDNUNGSART[eingang.art]}
                {eingang.art === 'MANUELL' && ` durch ${eingang.erstelltVon}`}
              </span>
              <Betrag betrag={eingang.betrag} waehrung={eingang.waehrung} />
              {hatRolle('BUCHHALTUNG') && (
                <Button
                  groesse="sm"
                  variante="dezent"
                  onClick={() => {
                    setAufzuheben(eingang)
                  }}
                >
                  Aufheben
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      <Dialog
        offen={aufzuheben !== null}
        titel="Zuordnung aufheben?"
        onSchliessen={() => {
          setAufzuheben(null)
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              onClick={() => {
                setAufzuheben(null)
              }}
            >
              Abbrechen
            </Button>
            <Button disabled={aufheben.isPending} onClick={() => void bestaetigen()}>
              Aufheben
            </Button>
          </>
        }
      >
        Die Zahlung geht zurück in die Klärungsliste, und der Betrag gilt bei Rechnung{' '}
        {rechnung.rechnungsnummer} wieder als offen.
      </Dialog>
    </section>
  )
}
