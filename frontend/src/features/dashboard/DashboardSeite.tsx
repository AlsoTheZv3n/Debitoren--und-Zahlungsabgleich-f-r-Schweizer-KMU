import { ChartColumn } from 'lucide-react'
import { useAktuellerBenutzer } from '@/shared/api/generiert/endpunkte'
import { useMandant } from '@/shared/lib/mandant'
import { ROLLEN, useRollen } from '@/shared/lib/rollen'
import { Badge } from '@/shared/ui/Badge'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

export function DashboardSeite() {
  const { data: benutzer } = useAktuellerBenutzer()
  const { aktiv } = useMandant()
  const { rollen, hauptrolle } = useRollen()

  return (
    <>
      <Seitenkopf
        titel="Dashboard"
        beschreibung={benutzer ? `Guten Tag, ${benutzer.anzeigename}` : undefined}
      />

      <section
        aria-labelledby="sitzung-titel"
        className="mb-6 rounded-lg border border-slate-200 bg-white"
      >
        <h2
          id="sitzung-titel"
          className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900"
        >
          Ihre Sitzung
        </h2>
        <dl className="grid gap-x-8 gap-y-4 px-4 py-4 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-slate-500">Angemeldet als</dt>
            <dd className="mt-1 font-medium text-slate-900">{benutzer?.anzeigename}</dd>
            <dd className="text-slate-500">{benutzer?.benutzername}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Mandant</dt>
            <dd className="mt-1 font-medium text-slate-900">{aktiv.name}</dd>
            <dd className="flex items-center gap-2 text-slate-500">
              {aktiv.ort}
              <Badge>{aktiv.referenzart === 'QR' ? 'QR-Referenz' : 'SCOR-Referenz'}</Badge>
            </dd>
          </div>
          <div>
            <dt className="text-slate-500">Rollen</dt>
            <dd className="mt-1 flex flex-wrap gap-1">
              {rollen.map((rolle) => (
                <Badge key={rolle} variante="brand">
                  {ROLLEN[rolle].name}
                </Badge>
              ))}
            </dd>
          </div>
        </dl>
        {hauptrolle && (
          <p className="border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
            <span className="font-medium text-slate-900">Berechtigung: </span>
            {ROLLEN[hauptrolle].darf}
          </p>
        )}
      </section>

      <LeererZustand
        symbol={ChartColumn}
        titel="Noch keine Kennzahlen"
        hinweis={<Badge>Folgt in Phase 4</Badge>}
      >
        Offene und überfällige Posten, die Altersstruktur und die letzten Zahlungseingänge
        erscheinen hier, sobald Rechnungen und Zahlungen erfasst sind.
      </LeererZustand>
    </>
  )
}
