import { AuditEintraege } from '@/features/audit/AuditEintraege'
import { useVerlaufDerRechnung } from '@/shared/api/generiert/endpunkte'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { fehlertext } from '@/shared/lib/http'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'

/** Verlauf aus dem Audit-Log: Freigabe, Zahlungseingänge, Mahnungen und Storno mit Benutzer und Zeitpunkt. */
export function RechnungVerlauf({ rechnung }: { rechnung: RechnungDto }) {
  const verlauf = useVerlaufDerRechnung(rechnung.id)

  return (
    <section aria-labelledby="verlauf" className="rounded-lg border border-slate-200 bg-white">
      <h2 id="verlauf" className="p-4 pb-2 text-sm font-semibold text-slate-900">
        Verlauf
      </h2>
      {verlauf.isPending ? (
        <Ladeanzeige className="px-4" />
      ) : verlauf.isError ? (
        <Meldung className="m-4">{fehlertext(verlauf.error)}</Meldung>
      ) : verlauf.data.length === 0 ? (
        <p className="px-4 pb-4 text-sm text-slate-500">
          Keine Einträge; das Audit-Log erfasst Änderungen seit seiner Einführung.
        </p>
      ) : (
        <div className="border-t border-slate-100">
          <AuditEintraege eintraege={verlauf.data} />
        </div>
      )}
    </section>
  )
}
