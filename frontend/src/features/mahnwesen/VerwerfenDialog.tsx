import { useState } from 'react'
import { useMahnungVerwerfen } from '@/shared/api/generiert/endpunkte'
import type { MahnungDto } from '@/shared/api/generiert/modell'
import { mahnungVerwerfenBodyGrundMax } from '@/shared/api/generiert/zod/mahnungen/mahnungen.zod'
import { heuteIso } from '@/shared/lib/format'
import { fehlertext } from '@/shared/lib/http'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Ankreuzfeld, Eingabe, Textbereich } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Meldung } from '@/shared/ui/Meldung'

/** Tage, die eine Rechnung nach dem Verwerfen standardmässig nicht mehr vorgeschlagen wird. */
const STANDARDSPERRE = 14

function inTagen(tage: number): string {
  const datum = new Date(`${heuteIso()}T00:00:00Z`)
  datum.setUTCDate(datum.getUTCDate() + tage)
  return datum.toISOString().slice(0, 10)
}

/**
 * Verwirft einen Vorschlag, etwa wenn der Kunde die Zahlung zugesagt hat. Mit Sperre schlägt der Mahnlauf die
 * Rechnung bis zum gewählten Tag nicht mehr vor.
 */
export function VerwerfenDialog({
  mahnung,
  onSchliessen,
  onVerworfen,
}: {
  mahnung: MahnungDto
  onSchliessen: () => void
  onVerworfen: () => Promise<void>
}) {
  const verwerfen = useMahnungVerwerfen()
  const [grund, setGrund] = useState('')
  const [sperren, setSperren] = useState(true)
  const [gesperrtBis, setGesperrtBis] = useState(inTagen(STANDARDSPERRE))
  const [fehler, setFehler] = useState<string | null>(null)

  const bestaetigen = async () => {
    setFehler(null)
    try {
      await verwerfen.mutateAsync({
        id: mahnung.id,
        data: {
          version: mahnung.version,
          grund: grund.trim() || undefined,
          gesperrtBis: sperren ? gesperrtBis : undefined,
        },
      })
      await onVerworfen()
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    }
  }

  return (
    <Dialog
      offen
      titel={`${mahnung.bezeichnung} zu ${mahnung.rechnungsnummer} verwerfen?`}
      onSchliessen={onSchliessen}
      aktionen={
        <>
          <Button variante="sekundaer" onClick={onSchliessen}>
            Abbrechen
          </Button>
          <Button disabled={verwerfen.isPending} onClick={() => void bestaetigen()}>
            Verwerfen
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {fehler && <Meldung>{fehler}</Meldung>}
        <p>Die Rechnung bleibt offen; es entsteht kein Schreiben und keine Gebühr.</p>
        <Feld label="Grund" optional>
          {(steuerung) => (
            <Textbereich
              {...steuerung}
              rows={2}
              maxLength={mahnungVerwerfenBodyGrundMax}
              value={grund}
              placeholder="Etwa: Kunde hat Zahlung zugesagt"
              onChange={(ereignis) => {
                setGrund(ereignis.target.value)
              }}
            />
          )}
        </Feld>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <Ankreuzfeld
            checked={sperren}
            onChange={(ereignis) => {
              setSperren(ereignis.target.checked)
            }}
          />
          Bis zu einem Datum nicht mehr vorschlagen
        </label>
        {sperren && (
          <Feld label="Nicht vorschlagen bis">
            {(steuerung) => (
              <Eingabe
                {...steuerung}
                type="date"
                min={heuteIso()}
                value={gesperrtBis}
                onChange={(ereignis) => {
                  setGesperrtBis(ereignis.target.value)
                }}
              />
            )}
          </Feld>
        )}
      </div>
    </Dialog>
  )
}
