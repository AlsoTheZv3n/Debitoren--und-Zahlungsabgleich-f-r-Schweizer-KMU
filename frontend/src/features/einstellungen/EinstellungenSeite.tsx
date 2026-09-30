import { useMandant } from '@/shared/lib/mandant'
import { Badge } from '@/shared/ui/Badge'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

/** Nur für Admins; die Route ist durch die RollenSchranke geschützt. */
export function EinstellungenSeite() {
  const { aktiv } = useMandant()

  return (
    <>
      <Seitenkopf titel="Einstellungen" beschreibung="Mandant, Mahnstufen und MWST-Sätze." />
      <section
        aria-labelledby="mandant-titel"
        className="rounded-lg border border-slate-200 bg-white"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h2 id="mandant-titel" className="text-sm font-semibold text-slate-900">
            Mandant
          </h2>
          <Badge>Bearbeiten folgt in Phase 4</Badge>
        </div>
        <dl className="grid gap-x-8 gap-y-3 px-4 py-4 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-slate-500">Name</dt>
            <dd className="mt-1 font-medium text-slate-900">{aktiv.name}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Ort</dt>
            <dd className="mt-1 text-slate-900">{aktiv.ort}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Zahlungsreferenz</dt>
            <dd className="mt-1 text-slate-900">
              {aktiv.referenzart === 'QR' ? 'QR-Referenz (QR-IBAN)' : 'SCOR-Referenz (IBAN)'}
            </dd>
          </div>
        </dl>
      </section>
    </>
  )
}
