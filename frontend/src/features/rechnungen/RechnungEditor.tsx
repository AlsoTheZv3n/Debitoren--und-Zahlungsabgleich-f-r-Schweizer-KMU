import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { Controller, useFieldArray, useForm, useWatch, type Path } from 'react-hook-form'
import { useBlocker, useNavigate } from 'react-router'
import { KundenAuswahl } from '@/features/kunden/KundenAuswahl'
import {
  getRechnungLesenQueryKey,
  useEntwurfAendern,
  useEntwurfAnlegen,
  useEntwurfLoeschen,
  useMwstSaetzeAuflisten,
  useRechnungFreigeben,
} from '@/shared/api/generiert/endpunkte'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { formatProzent } from '@/shared/lib/format'
import { ApiFehler, fehlertext, invalidieren } from '@/shared/lib/http'
import { Betrag } from '@/shared/ui/Betrag'
import { Button, IconButton, LinkButton } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Auswahl, Eingabe, Textbereich } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Kopfzelle, Tabelle, Zelle } from '@/shared/ui/Tabelle'
import { MWST_BEZEICHNUNG, MWST_CODES } from './mwst'
import {
  alsEntwurf,
  alsFormularpfad,
  alsFormularwerte,
  alsVorschauanfrage,
  LEERE_POSITION,
  rechnungSchema,
  type RechnungFormularAusgabe,
  type RechnungFormularWerte,
} from './rechnung-formular'
import { SummenBlock } from './SummenBlock'
import { useRechnungsvorschau } from './useRechnungsvorschau'
import { ZahlteilBild } from './ZahlteilBild'

type Rueckfrage = 'freigeben' | 'loeschen' | null

/**
 * Editor für neue Rechnungen und gespeicherte Entwürfe. Rechts stehen Summen und QR-Zahlteil, wie das
 * Backend sie für die aktuelle Eingabe berechnet; freigegeben wird nach einer Rückfrage.
 */
export function RechnungEditor({
  rechnung,
  startwerte,
  meldung,
}: {
  /** Der gespeicherte Entwurf; fehlt bei einer neuen Rechnung. */
  rechnung?: RechnungDto
  startwerte: RechnungFormularWerte
  meldung?: string
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [serverfehler, setServerfehler] = useState<string | null>(null)
  const [hinweis, setHinweis] = useState(meldung ?? null)
  const [rueckfrage, setRueckfrage] = useState<Rueckfrage>(null)
  const [freigabeWerte, setFreigabeWerte] = useState<RechnungFormularAusgabe | null>(null)
  const verlassenErlaubt = useRef(false)

  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<RechnungFormularWerte, unknown, RechnungFormularAusgabe>({
    resolver: zodResolver(rechnungSchema),
    defaultValues: startwerte,
  })
  const positionen = useFieldArray({ control, name: 'positionen' })
  const [kunde, rechnungsdatum, zahlungsfristTage, waehrung, positionswerte] = useWatch({
    control,
    name: ['kunde', 'rechnungsdatum', 'zahlungsfristTage', 'waehrung', 'positionen'],
  })
  const vorschau = useRechnungsvorschau(
    alsVorschauanfrage({
      kunde,
      rechnungsdatum,
      zahlungsfristTage,
      waehrung,
      positionen: positionswerte,
    }),
  )
  const saetze = useMwstSaetzeAuflisten({ stichtag: rechnungsdatum || undefined })

  const anlegen = useEntwurfAnlegen()
  const aendern = useEntwurfAendern()
  const freigeben = useRechnungFreigeben()
  const loeschen = useEntwurfLoeschen()
  const beschaeftigt =
    isSubmitting ||
    anlegen.isPending ||
    aendern.isPending ||
    freigeben.isPending ||
    loeschen.isPending

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      isDirty && !verlassenErlaubt.current && currentLocation.pathname !== nextLocation.pathname,
  )

  const fehlerZeigen = (fehler: unknown) => {
    if (fehler instanceof ApiFehler && fehler.fehler?.length) {
      for (const feldfehler of fehler.fehler) {
        setError(alsFormularpfad(feldfehler.feld) as Path<RechnungFormularWerte>, {
          message: feldfehler.meldung,
        })
      }
    }
    setServerfehler(fehlertext(fehler))
  }

  /** Speichert neue oder geänderte Eingaben; ein unveränderter Entwurf bleibt, wie er ist. */
  const speichern = async (werte: RechnungFormularAusgabe): Promise<RechnungDto> => {
    const gespeichert = !rechnung
      ? await anlegen.mutateAsync({ data: alsEntwurf(werte) })
      : isDirty
        ? await aendern.mutateAsync({
            id: rechnung.id,
            data: { version: rechnung.version, entwurf: alsEntwurf(werte) },
          })
        : rechnung
    queryClient.setQueryData(getRechnungLesenQueryKey(gespeichert.id), gespeichert)
    return gespeichert
  }

  const entwurfSpeichern = async (werte: RechnungFormularAusgabe) => {
    setServerfehler(null)
    setHinweis(null)
    try {
      const gespeichert = await speichern(werte)
      await invalidieren(queryClient, '/api/rechnungen')
      if (rechnung) {
        reset(alsFormularwerte(gespeichert))
        setHinweis('Entwurf gespeichert.')
      } else {
        verlassenErlaubt.current = true
        void navigate(`/rechnungen/${gespeichert.id}`, {
          replace: true,
          state: { meldung: 'Entwurf gespeichert.' },
        })
      }
    } catch (fehler) {
      fehlerZeigen(fehler)
    }
  }

  const freigabePruefen = handleSubmit((werte) => {
    setServerfehler(null)
    setFreigabeWerte(werte)
    setRueckfrage('freigeben')
  })

  const freigabeBestaetigen = async () => {
    if (!freigabeWerte) {
      return
    }
    try {
      const gespeichert = await speichern(freigabeWerte)
      const freigegeben = await freigeben.mutateAsync({
        id: gespeichert.id,
        data: { version: gespeichert.version },
      })
      queryClient.setQueryData(getRechnungLesenQueryKey(freigegeben.id), freigegeben)
      await invalidieren(queryClient, '/api/rechnungen')
      verlassenErlaubt.current = true
      setRueckfrage(null)
      void navigate(`/rechnungen/${freigegeben.id}`, {
        replace: true,
        state: { meldung: `Rechnung ${freigegeben.rechnungsnummer ?? ''} ist freigegeben.` },
      })
    } catch (fehler) {
      setRueckfrage(null)
      fehlerZeigen(fehler)
    }
  }

  const loeschenBestaetigen = async () => {
    if (!rechnung) {
      return
    }
    try {
      await loeschen.mutateAsync({ id: rechnung.id, params: { version: rechnung.version } })
      queryClient.removeQueries({ queryKey: getRechnungLesenQueryKey(rechnung.id) })
      await invalidieren(queryClient, '/api/rechnungen')
      verlassenErlaubt.current = true
      void navigate('/rechnungen', { replace: true })
    } catch (fehler) {
      setRueckfrage(null)
      fehlerZeigen(fehler)
    }
  }

  const satzText = (code: (typeof MWST_CODES)[number]) => {
    const satz = saetze.data?.find((eintrag) => eintrag.code === code)
    return code === 'OHNE' || !satz
      ? MWST_BEZEICHNUNG[code]
      : `${formatProzent(satz.satz)} ${MWST_BEZEICHNUNG[code]}`
  }

  const positionsbetrag = (zeile: number) => {
    const index = vorschau.data?.zeilen.indexOf(zeile) ?? -1
    return index < 0 ? undefined : vorschau.data?.positionsbetraege[index]
  }

  return (
    <>
      <Seitenkopf
        titel={rechnung ? 'Rechnungsentwurf' : 'Neue Rechnung'}
        beschreibung="Positionen erfassen; Summen und QR-Zahlteil rechnet das Backend laufend nach."
      />
      <form
        noValidate
        onSubmit={(ereignis) => void handleSubmit(entwurfSpeichern)(ereignis)}
        className="grid grid-cols-1 items-start gap-6 min-[112rem]:grid-cols-[minmax(0,1fr)_26rem]"
      >
        {/* Erst auf sehr breiten Bildschirmen steht der Zahlteil neben dem Formular; sonst brauchen
            die Positionen die ganze Breite und der Zahlteil folgt vor den Aktionen. */}
        <div className="space-y-6 min-[112rem]:col-start-1 min-[112rem]:row-start-1">
          {serverfehler && <Meldung>{serverfehler}</Meldung>}
          {hinweis && !serverfehler && <Meldung art="erfolg">{hinweis}</Meldung>}

          <section
            aria-labelledby="angaben"
            className="rounded-lg border border-slate-200 bg-white p-4"
          >
            <h2 id="angaben" className="mb-4 text-sm font-semibold text-slate-900">
              Angaben
            </h2>
            <div className="grid gap-4 sm:grid-cols-6">
              <Feld label="Kunde" className="sm:col-span-6" fehler={errors.kunde?.message}>
                {(steuerung) => (
                  <Controller
                    control={control}
                    name="kunde"
                    render={({ field }) => (
                      <KundenAuswahl
                        {...steuerung}
                        wert={field.value}
                        onWahl={field.onChange}
                        disabled={beschaeftigt}
                      />
                    )}
                  />
                )}
              </Feld>
              <Feld
                label="Rechnungsdatum"
                className="sm:col-span-2"
                fehler={errors.rechnungsdatum?.message}
              >
                {(steuerung) => (
                  <Eingabe {...steuerung} type="date" {...register('rechnungsdatum')} />
                )}
              </Feld>
              <Feld
                label="Zahlungsfrist in Tagen"
                optional
                className="sm:col-span-2"
                hinweis={
                  kunde?.zahlungsfristTage === undefined
                    ? 'Leer lassen für die Frist des Kunden'
                    : `Leer lassen für ${kunde.zahlungsfristTage} Tage wie beim Kunden`
                }
                fehler={errors.zahlungsfristTage?.message}
              >
                {(steuerung) => (
                  <Eingabe {...steuerung} inputMode="numeric" {...register('zahlungsfristTage')} />
                )}
              </Feld>
              <Feld label="Währung" className="sm:col-span-2" fehler={errors.waehrung?.message}>
                {(steuerung) => (
                  <Auswahl {...steuerung} {...register('waehrung')}>
                    <option value="CHF">CHF</option>
                    <option value="EUR">EUR</option>
                  </Auswahl>
                )}
              </Feld>
            </div>
          </section>

          <section
            aria-labelledby="positionen"
            className="rounded-lg border border-slate-200 bg-white"
          >
            <h2 id="positionen" className="p-4 pb-2 text-sm font-semibold text-slate-900">
              Positionen
            </h2>
            {(errors.positionen?.root?.message ?? errors.positionen?.message) && (
              <Meldung className="mx-4 mb-2">
                {errors.positionen.root?.message ?? errors.positionen.message}
              </Meldung>
            )}
            {/* Festes Layout: Die Beschreibung erhält den Platz, den die übrigen Spalten übrig lassen. */}
            <Tabelle className="min-w-[52rem] table-fixed">
              <thead>
                <tr>
                  <Kopfzelle className="w-10">#</Kopfzelle>
                  <Kopfzelle>Beschreibung</Kopfzelle>
                  <Kopfzelle className="w-24 text-right">Menge</Kopfzelle>
                  <Kopfzelle className="w-20">Einheit</Kopfzelle>
                  <Kopfzelle className="w-28 text-right">Einzelpreis</Kopfzelle>
                  <Kopfzelle className="w-52">MWST</Kopfzelle>
                  <Kopfzelle className="w-32 text-right">Betrag</Kopfzelle>
                  <Kopfzelle className="w-16">
                    <span className="sr-only">Aktionen</span>
                  </Kopfzelle>
                </tr>
              </thead>
              <tbody>
                {positionen.fields.map((feld, zeile) => {
                  const nummer = zeile + 1
                  const fehler = errors.positionen?.[zeile]
                  const betrag = positionsbetrag(zeile)
                  return (
                    <tr key={feld.id}>
                      <Zelle className="pt-4 text-slate-500 tabular-nums">{nummer}</Zelle>
                      <Zelle>
                        <Feld
                          label={`Beschreibung, Position ${nummer}`}
                          labelVersteckt
                          fehler={fehler?.beschreibung?.message}
                        >
                          {(steuerung) => (
                            <Eingabe
                              {...steuerung}
                              {...register(`positionen.${zeile}.beschreibung`)}
                            />
                          )}
                        </Feld>
                      </Zelle>
                      <Zelle>
                        <Feld
                          label={`Menge, Position ${nummer}`}
                          labelVersteckt
                          fehler={fehler?.menge?.message}
                        >
                          {(steuerung) => (
                            <Eingabe
                              {...steuerung}
                              inputMode="decimal"
                              className="text-right tabular-nums"
                              {...register(`positionen.${zeile}.menge`)}
                            />
                          )}
                        </Feld>
                      </Zelle>
                      <Zelle>
                        <Feld
                          label={`Einheit, Position ${nummer}`}
                          labelVersteckt
                          fehler={fehler?.einheit?.message}
                        >
                          {(steuerung) => (
                            <Eingabe
                              {...steuerung}
                              placeholder="Std."
                              {...register(`positionen.${zeile}.einheit`)}
                            />
                          )}
                        </Feld>
                      </Zelle>
                      <Zelle>
                        <Feld
                          label={`Einzelpreis, Position ${nummer}`}
                          labelVersteckt
                          fehler={fehler?.einzelpreis?.message}
                        >
                          {(steuerung) => (
                            <Eingabe
                              {...steuerung}
                              inputMode="decimal"
                              placeholder="0.00"
                              className="text-right tabular-nums"
                              {...register(`positionen.${zeile}.einzelpreis`)}
                            />
                          )}
                        </Feld>
                      </Zelle>
                      <Zelle>
                        <Feld
                          label={`MWST, Position ${nummer}`}
                          labelVersteckt
                          fehler={fehler?.mwstCode?.message}
                        >
                          {(steuerung) => (
                            <Auswahl {...steuerung} {...register(`positionen.${zeile}.mwstCode`)}>
                              {MWST_CODES.map((code) => (
                                <option key={code} value={code}>
                                  {satzText(code)}
                                </option>
                              ))}
                            </Auswahl>
                          )}
                        </Feld>
                      </Zelle>
                      <Zelle className="pt-4 text-right">
                        {betrag === undefined ? (
                          <span className="text-slate-500">–</span>
                        ) : (
                          <Betrag betrag={betrag} waehrung={waehrung} />
                        )}
                      </Zelle>
                      <Zelle>
                        <IconButton
                          variante="dezent"
                          aria-label={`Position ${nummer} entfernen`}
                          disabled={positionen.fields.length === 1}
                          onClick={() => {
                            positionen.remove(zeile)
                          }}
                        >
                          <Trash2 aria-hidden className="size-4" />
                        </IconButton>
                      </Zelle>
                    </tr>
                  )
                })}
              </tbody>
            </Tabelle>
            <div className="flex flex-wrap items-start justify-between gap-4 p-3">
              <Button
                variante="dezent"
                groesse="sm"
                onClick={() => {
                  positionen.append({ ...LEERE_POSITION })
                }}
              >
                <Plus aria-hidden className="size-4" />
                Position hinzufügen
              </Button>
              <section aria-labelledby="summen" className="w-full max-w-sm space-y-2 sm:px-1">
                <div className="flex items-baseline justify-between">
                  <h3 id="summen" className="text-sm font-semibold text-slate-900">
                    Summen
                  </h3>
                  {vorschau.isFetching && (
                    <span role="status" className="text-xs text-slate-500">
                      wird aktualisiert…
                    </span>
                  )}
                </div>
                {vorschau.isError && <Meldung>{fehlertext(vorschau.error)}</Meldung>}
                {vorschau.data ? (
                  <SummenBlock summen={vorschau.data.summen} waehrung={waehrung} />
                ) : (
                  !vorschau.isError && (
                    <p className="text-sm text-slate-500">
                      Die Summen erscheinen, sobald ein gültiges Rechnungsdatum erfasst ist.
                    </p>
                  )
                )}
              </section>
            </div>
          </section>

          <section
            aria-labelledby="bemerkung"
            className="rounded-lg border border-slate-200 bg-white p-4"
          >
            <h2 id="bemerkung" className="sr-only">
              Bemerkung
            </h2>
            <Feld label="Bemerkung auf der Rechnung" optional fehler={errors.bemerkung?.message}>
              {(steuerung) => <Textbereich {...steuerung} rows={3} {...register('bemerkung')} />}
            </Feld>
          </section>
        </div>

        <aside
          aria-labelledby="zahlteil"
          className="space-y-3 rounded-lg border border-slate-200 bg-white p-4 min-[112rem]:sticky min-[112rem]:top-4 min-[112rem]:col-start-2 min-[112rem]:row-span-2 min-[112rem]:row-start-1"
        >
          <h2 id="zahlteil" className="text-sm font-semibold text-slate-900">
            QR-Zahlteil
          </h2>
          {vorschau.data ? (
            <>
              <ZahlteilBild
                svg={vorschau.data.zahlteilSvg}
                beschriftung="Vorschau des QR-Zahlteils"
                className="max-w-3xl"
              />
              <p className="text-xs text-slate-500">
                Die Referenz vergibt das System bei der Freigabe; die Vorschau zeigt eine
                Beispielreferenz.
              </p>
            </>
          ) : (
            <p className="text-sm text-slate-500">
              Die Vorschau erscheint, sobald ein gültiges Rechnungsdatum erfasst ist.
            </p>
          )}
        </aside>

        <div className="flex flex-wrap items-center gap-2 min-[112rem]:col-start-1 min-[112rem]:row-start-2">
          {rechnung && (
            <Button
              variante="dezent"
              className="text-red-700 hover:bg-red-50 hover:text-red-800"
              disabled={beschaeftigt}
              onClick={() => {
                setRueckfrage('loeschen')
              }}
            >
              <Trash2 aria-hidden className="size-4" />
              Entwurf löschen
            </Button>
          )}
          <div className="ml-auto flex flex-wrap gap-2">
            <LinkButton variante="sekundaer" to="/rechnungen">
              Abbrechen
            </LinkButton>
            <Button type="submit" variante="sekundaer" disabled={beschaeftigt}>
              Entwurf speichern
            </Button>
            <Button disabled={beschaeftigt} onClick={() => void freigabePruefen()}>
              Freigeben
            </Button>
          </div>
        </div>
      </form>

      <Dialog
        offen={rueckfrage === 'freigeben'}
        titel="Rechnung freigeben?"
        onSchliessen={() => {
          setRueckfrage(null)
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              onClick={() => {
                setRueckfrage(null)
              }}
            >
              Abbrechen
            </Button>
            <Button disabled={beschaeftigt} onClick={() => void freigabeBestaetigen()}>
              Freigeben
            </Button>
          </>
        }
      >
        Die Rechnung erhält eine Nummer und eine Zahlungsreferenz. Danach lässt sie sich nicht mehr
        ändern, nur noch stornieren.
      </Dialog>

      <Dialog
        offen={rueckfrage === 'loeschen'}
        titel="Entwurf löschen?"
        onSchliessen={() => {
          setRueckfrage(null)
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              onClick={() => {
                setRueckfrage(null)
              }}
            >
              Abbrechen
            </Button>
            <Button
              className="bg-red-700 hover:bg-red-800"
              disabled={beschaeftigt}
              onClick={() => void loeschenBestaetigen()}
            >
              Löschen
            </Button>
          </>
        }
      >
        Der Entwurf wird endgültig entfernt.
      </Dialog>

      <Dialog
        offen={blocker.state === 'blocked'}
        titel="Änderungen verwerfen?"
        onSchliessen={() => {
          blocker.reset?.()
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              onClick={() => {
                blocker.reset?.()
              }}
            >
              Weiter bearbeiten
            </Button>
            <Button
              onClick={() => {
                blocker.proceed?.()
              }}
            >
              Verwerfen
            </Button>
          </>
        }
      >
        Die Rechnung hat ungespeicherte Änderungen.
      </Dialog>
    </>
  )
}
