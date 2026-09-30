import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import {
  getMahnstufenAuflistenQueryKey,
  useMahnstufenAuflisten,
  useMahnstufenFestlegen,
} from '@/shared/api/generiert/endpunkte'
import type { MahnstufeDto } from '@/shared/api/generiert/modell'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { Button, IconButton } from '@/shared/ui/Button'
import { Eingabe, Textbereich } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import {
  MAX_STUFEN,
  PLATZHALTER,
  mahnstufenAlsAnfrage,
  mahnstufenAlsWerte,
  mahnstufenSchema,
  neueStufe,
  type MahnstufenAusgabe,
  type MahnstufenWerte,
} from './einstellungen-formulare'

export function MahnstufenEinstellungen() {
  const stufen = useMahnstufenAuflisten()
  const [gespeichert, setGespeichert] = useState(false)

  if (stufen.isPending) {
    return <Ladeanzeige />
  }
  if (stufen.isError) {
    return <Meldung>{fehlertext(stufen.error)}</Meldung>
  }
  return (
    <>
      {gespeichert && (
        <Meldung art="erfolg" className="mb-4">
          Die Mahnstufen sind gespeichert; sie gelten ab dem nächsten Mahnlauf.
        </Meldung>
      )}
      <MahnstufenFormular
        key={stufen.data.map((stufe) => `${stufe.id}:${String(stufe.version)}`).join()}
        bisher={stufen.data}
        onGespeichert={() => {
          setGespeichert(true)
        }}
      />
    </>
  )
}

/**
 * Alle Stufen auf einmal. Die Reihenfolge ergibt die Stufe; versandte Mahnungen behalten Bezeichnung, Frist und
 * Gebühr ihrer Stufe.
 */
function MahnstufenFormular({
  bisher,
  onGespeichert,
}: {
  bisher: MahnstufeDto[]
  onGespeichert: () => void
}) {
  const queryClient = useQueryClient()
  const festlegen = useMahnstufenFestlegen()
  const [fehler, setFehler] = useState<string | null>(null)
  const {
    control,
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<MahnstufenWerte, unknown, MahnstufenAusgabe>({
    resolver: zodResolver(mahnstufenSchema),
    defaultValues: mahnstufenAlsWerte(bisher),
  })
  const { fields: stufen, append, remove } = useFieldArray({ control, name: 'stufen' })

  const speichern = handleSubmit(async (werte) => {
    setFehler(null)
    try {
      const neu = await festlegen.mutateAsync({ data: mahnstufenAlsAnfrage(werte, bisher) })
      queryClient.setQueryData(getMahnstufenAuflistenQueryKey(), neu)
      await invalidieren(queryClient, '/api/audit')
      onGespeichert()
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    }
  })

  return (
    <form onSubmit={(ereignis) => void speichern(ereignis)} noValidate className="space-y-4">
      {fehler && <Meldung>{fehler}</Meldung>}
      <p className="text-sm text-slate-600">
        Der tägliche Mahnlauf schlägt die nächste Stufe vor, sobald eine Rechnung so viele Tage
        überfällig ist und die letzte Mahnung mindestens den Abstand zurückliegt. Gebühren gelten in
        der Währung der Rechnung.
      </p>
      <ol className="space-y-4">
        {stufen.map((feld, index) => {
          const fehlerDerStufe = errors.stufen?.[index]
          return (
            <li key={feld.id}>
              <section
                aria-labelledby={`stufe-${feld.id}`}
                className="rounded-lg border border-slate-200 bg-white"
              >
                <div className="flex items-center justify-between border-b border-slate-200 px-4 py-2">
                  <h2 id={`stufe-${feld.id}`} className="text-sm font-semibold text-slate-900">
                    Stufe {index + 1}
                  </h2>
                  {stufen.length > 1 && (
                    <IconButton
                      variante="dezent"
                      groesse="sm"
                      aria-label={`Stufe ${String(index + 1)} entfernen`}
                      onClick={() => {
                        remove(index)
                      }}
                    >
                      <Trash2 aria-hidden className="size-4" />
                    </IconButton>
                  )}
                </div>
                <div className="grid gap-4 p-4 sm:grid-cols-12">
                  <Feld
                    label="Bezeichnung"
                    className="sm:col-span-4"
                    fehler={fehlerDerStufe?.bezeichnung?.message}
                  >
                    {(steuerung) => (
                      <Eingabe {...steuerung} {...register(`stufen.${index}.bezeichnung`)} />
                    )}
                  </Feld>
                  <Feld
                    label="Nach Fälligkeit"
                    hinweis="Tage"
                    className="sm:col-span-2"
                    fehler={fehlerDerStufe?.tageNachFaelligkeit?.message}
                  >
                    {(steuerung) => (
                      <Eingabe
                        {...steuerung}
                        type="number"
                        min={1}
                        {...register(`stufen.${index}.tageNachFaelligkeit`, {
                          valueAsNumber: true,
                        })}
                      />
                    )}
                  </Feld>
                  <Feld
                    label="Abstand"
                    hinweis={index === 0 ? 'Bei Stufe 1 ohne Wirkung' : 'Tage seit der letzten'}
                    className="sm:col-span-2"
                    fehler={fehlerDerStufe?.mindestabstandTage?.message}
                  >
                    {(steuerung) => (
                      <Eingabe
                        {...steuerung}
                        type="number"
                        min={0}
                        // Nur lesbar statt gesperrt: Gesperrte Felder liefert das Formular nicht mit.
                        readOnly={index === 0}
                        className={index === 0 ? 'bg-slate-50 text-slate-500' : undefined}
                        {...register(`stufen.${index}.mindestabstandTage`, { valueAsNumber: true })}
                      />
                    )}
                  </Feld>
                  <Feld
                    label="Frist"
                    hinweis="Tage ab Mahndatum"
                    className="sm:col-span-2"
                    fehler={fehlerDerStufe?.fristTage?.message}
                  >
                    {(steuerung) => (
                      <Eingabe
                        {...steuerung}
                        type="number"
                        min={1}
                        {...register(`stufen.${index}.fristTage`, { valueAsNumber: true })}
                      />
                    )}
                  </Feld>
                  <Feld
                    label="Gebühr"
                    hinweis="0 für keine"
                    className="sm:col-span-2"
                    fehler={fehlerDerStufe?.gebuehr?.message}
                  >
                    {(steuerung) => (
                      <Eingabe
                        {...steuerung}
                        inputMode="decimal"
                        className="text-right tabular-nums"
                        {...register(`stufen.${index}.gebuehr`)}
                      />
                    )}
                  </Feld>
                  <Feld
                    label="Brieftext"
                    className="sm:col-span-12"
                    hinweis={`Platzhalter: ${PLATZHALTER.join(', ')}. Eine Leerzeile beginnt einen neuen Absatz.`}
                    fehler={fehlerDerStufe?.textvorlage?.message}
                  >
                    {(steuerung) => (
                      <Textbereich
                        {...steuerung}
                        rows={4}
                        {...register(`stufen.${index}.textvorlage`)}
                      />
                    )}
                  </Feld>
                </div>
              </section>
            </li>
          )
        })}
      </ol>
      {errors.stufen?.root?.message && <Meldung>{errors.stufen.root.message}</Meldung>}
      <div className="flex flex-wrap justify-between gap-2">
        <Button
          variante="sekundaer"
          disabled={stufen.length >= MAX_STUFEN}
          onClick={() => {
            append(neueStufe(getValues()))
          }}
        >
          <Plus aria-hidden className="size-4" />
          Stufe hinzufügen
        </Button>
        <Button type="submit" disabled={isSubmitting || !isDirty}>
          {isSubmitting ? 'Wird gespeichert…' : 'Mahnstufen speichern'}
        </Button>
      </div>
    </form>
  )
}
