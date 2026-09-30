import { useQueryClient } from '@tanstack/react-query'
import { useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import {
  getAktuellerBenutzerQueryKey,
  getMandantenAuflistenQueryKey,
  useMandantenAuflisten,
} from '@/shared/api/generiert/endpunkte'
import { useAnmeldung } from '@/shared/lib/anmeldung'
import { ApiFehler, setzeMandant } from '@/shared/lib/http'
import { MandantKontext, mandantSpeicher, type MandantAuswahl } from '@/shared/lib/mandant'
import { Button } from '@/shared/ui/Button'
import { Vollbildmeldung } from '@/shared/ui/Vollbildmeldung'

/** Abfragen, die nicht vom Mandanten abhängen und beim Wechsel bleiben dürfen. */
const MANDANTENUNABHAENGIG = new Set<unknown>([
  getAktuellerBenutzerQueryKey()[0],
  getMandantenAuflistenQueryKey()[0],
])

/**
 * Bestimmt den Mandanten, für den gearbeitet wird, und setzt ihn als Header für alle API-Aufrufe. Beim
 * Wechsel werden alle mandantenbezogenen Daten verworfen, damit nie Daten des vorherigen Mandanten
 * sichtbar bleiben.
 */
export function MandantProvider({ children }: { children: ReactNode }) {
  const abfrage = useMandantenAuflisten()
  const queryClient = useQueryClient()
  const { abmelden } = useAnmeldung()
  const [gewaehlt, setGewaehlt] = useState(() => mandantSpeicher.lesen())

  const mandanten = abfrage.data
  const aktiv = mandanten?.find((m) => m.id === gewaehlt) ?? mandanten?.[0]

  // Layout-Effekt: läuft vor den Effekten der Kinder, deren erste Abfrage trägt also schon den Header.
  useLayoutEffect(() => {
    setzeMandant(aktiv?.id ?? null)
  }, [aktiv?.id])

  const auswahl = useMemo<MandantAuswahl | null>(
    () =>
      mandanten && aktiv
        ? {
            mandanten,
            aktiv,
            waehlen: (id) => {
              setzeMandant(id)
              mandantSpeicher.schreiben(id)
              setGewaehlt(id)
              void queryClient.resetQueries({
                predicate: (query) => !MANDANTENUNABHAENGIG.has(query.queryKey[0]),
              })
            },
          }
        : null,
    [mandanten, aktiv, queryClient],
  )

  if (abfrage.isPending) {
    return <Vollbildmeldung titel="Mandanten werden geladen" laedt />
  }
  if (abfrage.isError) {
    const keineRolle = abfrage.error instanceof ApiFehler && abfrage.error.status === 403
    return (
      <Vollbildmeldung
        titel={keineRolle ? 'Keine Berechtigung' : 'Mandanten nicht verfügbar'}
        aktion={
          <Button variante="sekundaer" onClick={abmelden}>
            Abmelden
          </Button>
        }
      >
        {keineRolle
          ? 'Ihrem Benutzer ist keine Rolle zugewiesen. Bitte wenden Sie sich an die Administration.'
          : 'Die Mandanten konnten nicht geladen werden. Bitte später erneut versuchen.'}
      </Vollbildmeldung>
    )
  }
  if (!auswahl) {
    return (
      <Vollbildmeldung
        titel="Kein Mandant zugewiesen"
        aktion={
          <Button variante="sekundaer" onClick={abmelden}>
            Abmelden
          </Button>
        }
      >
        Ihrem Benutzer ist noch kein Mandant zugewiesen. Bitte wenden Sie sich an die
        Administration.
      </Vollbildmeldung>
    )
  }
  return (
    <MandantKontext value={auswahl}>
      {/* Neuer Schlüssel je Mandant: lokaler Zustand der Seiten beginnt nach dem Wechsel frisch. */}
      <div key={auswahl.aktiv.id} className="contents">
        {children}
      </div>
    </MandantKontext>
  )
}
