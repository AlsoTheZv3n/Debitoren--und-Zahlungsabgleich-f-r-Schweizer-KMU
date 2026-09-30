import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm, useWatch, type Path } from 'react-hook-form'
import {
  getMandantLesenQueryKey,
  useMandantAendern,
  useMandantLesen,
} from '@/shared/api/generiert/endpunkte'
import type { MandantDetailDto } from '@/shared/api/generiert/modell'
import { ApiFehler, fehlertext, invalidieren } from '@/shared/lib/http'
import { Button } from '@/shared/ui/Button'
import { Ankreuzfeld, Eingabe } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import {
  mandantAlsAenderung,
  mandantAlsWerte,
  mandantSchema,
  type MandantAusgabe,
  type MandantWerte,
} from './einstellungen-formulare'

export function MandantEinstellungen() {
  const mandant = useMandantLesen()
  const [gespeichert, setGespeichert] = useState(false)

  if (mandant.isPending) {
    return <Ladeanzeige />
  }
  if (mandant.isError) {
    return <Meldung>{fehlertext(mandant.error)}</Meldung>
  }
  // Nach dem Speichern beginnt das Formular mit dem neuen Stand und seiner Version.
  return (
    <>
      {gespeichert && (
        <Meldung art="erfolg" className="mb-4">
          Die Angaben des Mandanten sind gespeichert.
        </Meldung>
      )}
      <MandantFormular
        key={mandant.data.version}
        mandant={mandant.data}
        onGespeichert={() => {
          setGespeichert(true)
        }}
      />
    </>
  )
}

/** Angaben, die auf jeder Rechnung und im QR-Zahlteil stehen; freigegebene Rechnungen bleiben, wie sie sind. */
function MandantFormular({
  mandant,
  onGespeichert,
}: {
  mandant: MandantDetailDto
  onGespeichert: () => void
}) {
  const queryClient = useQueryClient()
  const aendern = useMandantAendern()
  const [fehler, setFehler] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<MandantWerte, unknown, MandantAusgabe>({
    resolver: zodResolver(mandantSchema),
    defaultValues: mandantAlsWerte(mandant),
  })
  const qrIban = useWatch({ control, name: 'qrIban' })

  const speichern = handleSubmit(async (werte) => {
    setFehler(null)
    try {
      const neu = await aendern.mutateAsync({ data: mandantAlsAenderung(werte, mandant.version) })
      queryClient.setQueryData(getMandantLesenQueryKey(), neu)
      await invalidieren(queryClient, '/api/mandanten')
      await invalidieren(queryClient, '/api/audit')
      onGespeichert()
    } catch (ausnahme) {
      if (ausnahme instanceof ApiFehler && ausnahme.fehler?.length) {
        for (const feldfehler of ausnahme.fehler) {
          setError(feldfehler.feld as Path<MandantWerte>, { message: feldfehler.meldung })
        }
      }
      setFehler(fehlertext(ausnahme))
    }
  })

  return (
    <section
      aria-labelledby="mandant-titel"
      className="rounded-lg border border-slate-200 bg-white"
    >
      <h2
        id="mandant-titel"
        className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900"
      >
        Mandant
      </h2>
      <form onSubmit={(ereignis) => void speichern(ereignis)} noValidate className="space-y-4 p-4">
        {fehler && <Meldung>{fehler}</Meldung>}
        <div className="grid gap-4 sm:grid-cols-6">
          <Feld label="Name" className="sm:col-span-6" fehler={errors.name?.message}>
            {(steuerung) => (
              <Eingabe {...steuerung} autoComplete="organization" {...register('name')} />
            )}
          </Feld>
          <Feld label="Strasse" optional className="sm:col-span-4" fehler={errors.strasse?.message}>
            {(steuerung) => <Eingabe {...steuerung} {...register('strasse')} />}
          </Feld>
          <Feld label="Nr." optional className="sm:col-span-2" fehler={errors.hausnummer?.message}>
            {(steuerung) => <Eingabe {...steuerung} {...register('hausnummer')} />}
          </Feld>
          <Feld label="PLZ" className="sm:col-span-2" fehler={errors.plz?.message}>
            {(steuerung) => <Eingabe {...steuerung} inputMode="numeric" {...register('plz')} />}
          </Feld>
          <Feld label="Ort" className="sm:col-span-3" fehler={errors.ort?.message}>
            {(steuerung) => <Eingabe {...steuerung} {...register('ort')} />}
          </Feld>
          <Feld label="Land" className="sm:col-span-1" fehler={errors.land?.message}>
            {(steuerung) => <Eingabe {...steuerung} maxLength={2} {...register('land')} />}
          </Feld>
          <Feld
            label="IBAN"
            className="sm:col-span-3"
            hinweis="Konto für Zahlungen ohne QR-Referenz"
            fehler={errors.iban?.message}
          >
            {(steuerung) => <Eingabe {...steuerung} className="font-mono" {...register('iban')} />}
          </Feld>
          <Feld
            label="QR-IBAN"
            optional
            className="sm:col-span-3"
            hinweis={
              qrIban.trim()
                ? 'Neue Rechnungen erhalten eine QR-Referenz'
                : 'Ohne QR-IBAN erhalten neue Rechnungen eine SCOR-Referenz'
            }
            fehler={errors.qrIban?.message}
          >
            {(steuerung) => (
              <Eingabe {...steuerung} className="font-mono" {...register('qrIban')} />
            )}
          </Feld>
          <Feld
            label="MWST-Nummer"
            optional
            className="sm:col-span-3"
            hinweis="Format CHE-123.456.789 MWST"
            fehler={errors.mwstNummer?.message}
          >
            {(steuerung) => <Eingabe {...steuerung} {...register('mwstNummer')} />}
          </Feld>
          <Feld
            label="E-Mail"
            optional
            className="sm:col-span-3"
            hinweis="Steht auf jeder Rechnung; Antworten auf Mahnungen gehen hierhin"
            fehler={errors.email?.message}
          >
            {(steuerung) => <Eingabe {...steuerung} type="email" {...register('email')} />}
          </Feld>
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <Ankreuzfeld {...register('rundungFuenfRappen')} />
          Rechnungen in CHF auf 5 Rappen runden
        </label>
        <p className="text-xs text-slate-500">
          Freigegebene Rechnungen behalten Konto, Referenz und PDF. Wer die QR-IBAN entfernt, sollte
          Bankdateien des alten Kontos zuerst noch importieren.
        </p>
        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting || !isDirty}>
            {isSubmitting ? 'Wird gespeichert…' : 'Speichern'}
          </Button>
        </div>
      </form>
    </section>
  )
}
