import { Link } from 'react-router'
import type { AuditEintragDto, AuditEintragDtoAktion } from '@/shared/api/generiert/modell'
import { formatZeitpunkt } from '@/shared/lib/format'
import { Badge, type BadgeVariante } from '@/shared/ui/Badge'
import { OBJEKTART } from './audit-texte'

const AKTION: Record<AuditEintragDtoAktion, { text: string; variante: BadgeVariante }> = {
  ANGELEGT: { text: 'angelegt', variante: 'brand' },
  GEAENDERT: { text: 'geändert', variante: 'neutral' },
  GELOESCHT: { text: 'gelöscht', variante: 'ueberfaellig' },
}

/**
 * Einträge des Audit-Logs als Liste: wer wann was geändert hat, mit den Werten vorher und nachher. Mit
 * {@code mitRechnungslink} führt die Bezeichnung zur betroffenen Rechnung.
 */
export function AuditEintraege({
  eintraege,
  mitRechnungslink = false,
}: {
  eintraege: AuditEintragDto[]
  mitRechnungslink?: boolean
}) {
  return (
    <ol className="divide-y divide-slate-100">
      {eintraege.map((eintrag) => (
        <li
          key={`${String(eintrag.revision)}-${eintrag.objektart}-${eintrag.objektId}`}
          className="px-4 py-2.5 text-sm"
        >
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="text-slate-500 tabular-nums">
              {formatZeitpunkt(eintrag.zeitpunkt)}
            </span>
            <span className="text-slate-500">·</span>
            <span className="font-medium text-slate-900">{eintrag.benutzer}</span>
            <Badge variante={AKTION[eintrag.aktion].variante}>
              {OBJEKTART[eintrag.objektart]} {AKTION[eintrag.aktion].text}
            </Badge>
            {mitRechnungslink && eintrag.rechnungId ? (
              <Link
                to={`/rechnungen/${eintrag.rechnungId}`}
                className="text-slate-700 hover:text-brand-700 hover:underline focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none"
              >
                {eintrag.bezeichnung}
              </Link>
            ) : (
              <span className="text-slate-700">{eintrag.bezeichnung}</span>
            )}
          </div>
          {eintrag.aenderungen.length > 0 && (
            <dl className="mt-1 grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-0.5 text-xs">
              {eintrag.aenderungen.map((aenderung) => (
                <div key={aenderung.feld} className="contents">
                  <dt className="text-slate-500">{aenderung.feld}</dt>
                  <dd className="text-slate-700">
                    {aenderung.vorher !== undefined && (
                      <>
                        <span className="text-slate-500 line-through">{aenderung.vorher}</span>
                        {aenderung.nachher !== undefined && (
                          <>
                            <span aria-hidden> → </span>
                            <span className="sr-only"> geändert zu </span>
                          </>
                        )}
                      </>
                    )}
                    {aenderung.nachher}
                  </dd>
                </div>
              ))}
            </dl>
          )}
        </li>
      ))}
    </ol>
  )
}
