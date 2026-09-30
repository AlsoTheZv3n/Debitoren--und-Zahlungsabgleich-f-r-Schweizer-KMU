import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { MWST_BEZEICHNUNG, MWST_CODES } from '@/features/rechnungen/mwst'
import { useMwstSaetzeAuflisten, useMwstSatzErfassen } from '@/shared/api/generiert/endpunkte'
import type { MwstSatzDto, MwstSatzDtoCode } from '@/shared/api/generiert/modell'
import { formatDatum, formatProzent, heuteIso } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { alsZahl, nachkommastellen } from '@/shared/lib/zahlen'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Auswahl, Eingabe } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { Kopfzelle, Tabelle, Zelle } from '@/shared/ui/Tabelle'

/** Datierte MWST-Sätze. Ein neuer Satz gilt ab einem Stichtag; der bisherige desselben Codes endet am Vortag. */
export function MwstEinstellungen() {
  const saetze = useMwstSaetzeAuflisten()
  const heute = heuteIso()

  return (
    <div className="space-y-6">
      <section
        aria-labelledby="saetze-titel"
        className="rounded-lg border border-slate-200 bg-white"
      >
        <h2
          id="saetze-titel"
          className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900"
        >
          MWST-Sätze
        </h2>
        {saetze.isPending ? (
          <Ladeanzeige className="px-4" />
        ) : saetze.isError ? (
          <Meldung className="m-4">{fehlertext(saetze.error)}</Meldung>
        ) : (
          <Tabelle>
            <thead>
              <tr>
                <Kopfzelle>Satz</Kopfzelle>
                <Kopfzelle className="text-right">Prozent</Kopfzelle>
                <Kopfzelle>Gültig ab</Kopfzelle>
                <Kopfzelle>Gültig bis</Kopfzelle>
                <Kopfzelle>
                  <span className="sr-only">Stand</span>
                </Kopfzelle>
              </tr>
            </thead>
            <tbody>
              {sortiert(saetze.data).map((satz) => (
                <tr key={satz.id}>
                  <Zelle>{MWST_BEZEICHNUNG[satz.code]}</Zelle>
                  <Zelle className="text-right tabular-nums">{formatProzent(satz.satz)}</Zelle>
                  <Zelle className="tabular-nums">{formatDatum(satz.gueltigAb)}</Zelle>
                  <Zelle className="tabular-nums">
                    {satz.gueltigBis ? formatDatum(satz.gueltigBis) : 'offen'}
                  </Zelle>
                  <Zelle>{standBadge(satz, heute)}</Zelle>
                </tr>
              ))}
            </tbody>
          </Tabelle>
        )}
      </section>
      <NeuerSatz />
    </div>
  )
}

function sortiert(saetze: MwstSatzDto[]): MwstSatzDto[] {
  return [...saetze].sort(
    (a, b) =>
      MWST_CODES.indexOf(a.code) - MWST_CODES.indexOf(b.code) ||
      a.gueltigAb.localeCompare(b.gueltigAb),
  )
}

function standBadge(satz: MwstSatzDto, heute: string) {
  if (satz.gueltigAb > heute) {
    return <Badge variante="brand">Ab {formatDatum(satz.gueltigAb)}</Badge>
  }
  if (satz.gueltigBis && satz.gueltigBis < heute) {
    return <Badge>Abgelaufen</Badge>
  }
  return <Badge variante="bezahlt">Gilt heute</Badge>
}

function NeuerSatz() {
  const queryClient = useQueryClient()
  const erfassen = useMwstSatzErfassen()
  const [code, setCode] = useState<MwstSatzDtoCode>('NORMAL')
  const [prozent, setProzent] = useState('')
  const [gueltigAb, setGueltigAb] = useState('')
  const [fehler, setFehler] = useState<Record<string, string>>({})
  const [meldung, setMeldung] = useState<{ art: 'erfolg' | 'fehler'; text: string } | null>(null)

  const speichern = async () => {
    const neu: Record<string, string> = {}
    const satz = alsZahl(prozent)
    if (satz === null || satz >= 100 || nachkommastellen(prozent) > 2) {
      neu.prozent = 'Satz zwischen 0 und 99.99 mit höchstens zwei Nachkommastellen'
    }
    if (!gueltigAb) {
      neu.gueltigAb = 'Stichtag angeben'
    }
    setFehler(neu)
    setMeldung(null)
    if (satz === null || Object.keys(neu).length > 0) {
      return
    }
    try {
      await erfassen.mutateAsync({ data: { code, satz, gueltigAb } })
      setMeldung({
        art: 'erfolg',
        text: `${MWST_BEZEICHNUNG[code]} ${formatProzent(satz)} gilt ab ${formatDatum(gueltigAb)}; der bisherige Satz endet am Vortag.`,
      })
      setProzent('')
      setGueltigAb('')
      await invalidieren(queryClient, '/api/mwst-saetze')
      await invalidieren(queryClient, '/api/audit')
    } catch (ausnahme) {
      setMeldung({ art: 'fehler', text: fehlertext(ausnahme) })
    }
  }

  return (
    <section aria-labelledby="neuer-satz" className="rounded-lg border border-slate-200 bg-white">
      <h2
        id="neuer-satz"
        className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900"
      >
        Neuer Satz ab Stichtag
      </h2>
      <form
        noValidate
        className="space-y-4 p-4"
        onSubmit={(ereignis) => {
          ereignis.preventDefault()
          void speichern()
        }}
      >
        {meldung && <Meldung art={meldung.art}>{meldung.text}</Meldung>}
        <div className="grid gap-4 sm:grid-cols-3">
          <Feld label="Satz">
            {(steuerung) => (
              <Auswahl
                {...steuerung}
                value={code}
                onChange={(ereignis) => {
                  setCode(ereignis.target.value as MwstSatzDtoCode)
                }}
              >
                {MWST_CODES.map((wert) => (
                  <option key={wert} value={wert}>
                    {MWST_BEZEICHNUNG[wert]}
                  </option>
                ))}
              </Auswahl>
            )}
          </Feld>
          <Feld label="Prozent" fehler={fehler.prozent}>
            {(steuerung) => (
              <Eingabe
                {...steuerung}
                inputMode="decimal"
                placeholder="8.1"
                value={prozent}
                onChange={(ereignis) => {
                  setProzent(ereignis.target.value)
                }}
              />
            )}
          </Feld>
          <Feld label="Gültig ab" fehler={fehler.gueltigAb}>
            {(steuerung) => (
              <Eingabe
                {...steuerung}
                type="date"
                value={gueltigAb}
                onChange={(ereignis) => {
                  setGueltigAb(ereignis.target.value)
                }}
              />
            )}
          </Feld>
        </div>
        <div className="flex justify-end">
          <Button type="submit" disabled={erfassen.isPending}>
            Satz erfassen
          </Button>
        </div>
      </form>
    </section>
  )
}
