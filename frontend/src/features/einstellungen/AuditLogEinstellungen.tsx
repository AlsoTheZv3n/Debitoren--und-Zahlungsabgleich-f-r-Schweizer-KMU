import { keepPreviousData } from '@tanstack/react-query'
import { History } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { AuditEintraege } from '@/features/audit/AuditEintraege'
import { OBJEKTART } from '@/features/audit/audit-texte'
import { useAuditLogLesen } from '@/shared/api/generiert/endpunkte'
import { AuditLogLesenObjektart } from '@/shared/api/generiert/modell'
import { fehlertext } from '@/shared/lib/http'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { Auswahl, Eingabe } from '@/shared/ui/Eingabe'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenwahl } from '@/shared/ui/Seitenwahl'

const alsObjektart = (wert: string | null) =>
  wert && wert in AuditLogLesenObjektart ? (wert as AuditLogLesenObjektart) : undefined

/** Wer wann was geändert hat, über alle Rechnungen, Zahlungen, Mahnungen und Einstellungen des Mandanten. */
export function AuditLogEinstellungen() {
  const [parameter, setParameter] = useSearchParams()
  const objektart = alsObjektart(parameter.get('objektart'))
  const benutzer = parameter.get('benutzer') ?? ''
  const seite = Number(parameter.get('seite') ?? '0')
  const [eingabe, setEingabe] = useState(benutzer)
  const entprellt = useEntprellt(eingabe)

  const setzen = useCallback(
    (name: string, wert: string | undefined) => {
      setParameter(
        (alt) => {
          const neu = new URLSearchParams(alt)
          if (wert) {
            neu.set(name, wert)
          } else {
            neu.delete(name)
          }
          neu.delete('seite')
          return neu
        },
        { replace: true },
      )
    },
    [setParameter],
  )

  useEffect(() => {
    if (entprellt.trim() !== benutzer) {
      setzen('benutzer', entprellt.trim() || undefined)
    }
  }, [entprellt, benutzer, setzen])

  const abfrage = useAuditLogLesen(
    { objektart, benutzer: benutzer || undefined, seite, groesse: 25 },
    { query: { placeholderData: keepPreviousData } },
  )

  return (
    <section aria-label="Audit-Log" className="rounded-lg border border-slate-200 bg-white">
      <div
        role="search"
        className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3"
      >
        <label className="flex items-center gap-2 text-sm text-slate-700">
          Objekt
          <Auswahl
            className="w-48"
            value={objektart ?? ''}
            onChange={(ereignis) => {
              setzen('objektart', ereignis.target.value || undefined)
            }}
          >
            <option value="">Alle</option>
            {Object.values(AuditLogLesenObjektart).map((wert) => (
              <option key={wert} value={wert}>
                {OBJEKTART[wert]}
              </option>
            ))}
          </Auswahl>
        </label>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          Benutzer
          <Eingabe
            className="w-44"
            value={eingabe}
            placeholder="etwa buchhaltung"
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
        <LeererZustand symbol={History} titel="Keine Einträge">
          {objektart || benutzer
            ? 'Keine Änderung passt zu diesen Filtern.'
            : 'Änderungen erscheinen hier, sobald jemand etwas anlegt oder ändert.'}
        </LeererZustand>
      ) : (
        <>
          <AuditEintraege eintraege={abfrage.data.inhalt} mitRechnungslink />
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
  )
}
