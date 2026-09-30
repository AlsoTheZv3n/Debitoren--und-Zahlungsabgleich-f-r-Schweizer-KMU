import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, type Path } from 'react-hook-form'
import { ApiFehler, fehlertext } from '@/shared/lib/http'
import { Button } from '@/shared/ui/Button'
import { Eingabe } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Meldung } from '@/shared/ui/Meldung'
import { kundeSchema, type KundeFormularAusgabe, type KundeFormularWerte } from './kunde-formular'

/** Formular für neue und bestehende Kunden; Fehler des Backends landen beim passenden Feld. */
export function KundeFormular({
  startwerte,
  kundennummerAenderbar,
  speichernText,
  onSpeichern,
  onAbbrechen,
}: {
  startwerte: KundeFormularWerte
  kundennummerAenderbar: boolean
  speichernText: string
  onSpeichern: (werte: KundeFormularAusgabe) => Promise<unknown>
  onAbbrechen: () => void
}) {
  const [serverfehler, setServerfehler] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<KundeFormularWerte, unknown, KundeFormularAusgabe>({
    resolver: zodResolver(kundeSchema),
    defaultValues: startwerte,
  })

  const absenden = handleSubmit(async (werte) => {
    setServerfehler(null)
    try {
      await onSpeichern(werte)
    } catch (fehler) {
      if (fehler instanceof ApiFehler && fehler.fehler?.length) {
        for (const feldfehler of fehler.fehler) {
          setError(feldfehler.feld as Path<KundeFormularWerte>, { message: feldfehler.meldung })
        }
      }
      setServerfehler(fehlertext(fehler))
    }
  })

  return (
    <form onSubmit={(ereignis) => void absenden(ereignis)} noValidate className="space-y-4">
      {serverfehler && <Meldung>{serverfehler}</Meldung>}
      <div className="grid gap-4 sm:grid-cols-6">
        <Feld
          label="Kundennummer"
          className="sm:col-span-2"
          optional={kundennummerAenderbar}
          hinweis={kundennummerAenderbar ? 'Leer lassen für die nächste freie Nummer' : undefined}
          fehler={errors.kundennummer?.message}
        >
          {(steuerung) => (
            <Eingabe
              {...steuerung}
              disabled={!kundennummerAenderbar}
              {...register('kundennummer')}
            />
          )}
        </Feld>
        <Feld label="Name" className="sm:col-span-4" fehler={errors.name?.message}>
          {(steuerung) => (
            <Eingabe {...steuerung} autoComplete="organization" {...register('name')} />
          )}
        </Feld>
        <Feld label="Strasse" optional className="sm:col-span-4" fehler={errors.strasse?.message}>
          {(steuerung) => (
            <Eingabe {...steuerung} autoComplete="address-line1" {...register('strasse')} />
          )}
        </Feld>
        <Feld label="Nr." optional className="sm:col-span-2" fehler={errors.hausnummer?.message}>
          {(steuerung) => <Eingabe {...steuerung} {...register('hausnummer')} />}
        </Feld>
        <Feld label="PLZ" className="sm:col-span-2" fehler={errors.plz?.message}>
          {(steuerung) => (
            <Eingabe
              {...steuerung}
              autoComplete="postal-code"
              inputMode="numeric"
              {...register('plz')}
            />
          )}
        </Feld>
        <Feld label="Ort" className="sm:col-span-3" fehler={errors.ort?.message}>
          {(steuerung) => (
            <Eingabe {...steuerung} autoComplete="address-level2" {...register('ort')} />
          )}
        </Feld>
        <Feld label="Land" className="sm:col-span-1" fehler={errors.land?.message}>
          {(steuerung) => <Eingabe {...steuerung} maxLength={2} {...register('land')} />}
        </Feld>
        <Feld label="E-Mail" optional className="sm:col-span-4" fehler={errors.email?.message}>
          {(steuerung) => (
            <Eingabe {...steuerung} type="email" autoComplete="email" {...register('email')} />
          )}
        </Feld>
        <Feld
          label="Zahlungsfrist"
          className="sm:col-span-2"
          hinweis="Tage ab Rechnungsdatum"
          fehler={errors.zahlungsfristTage?.message}
        >
          {(steuerung) => (
            <Eingabe
              {...steuerung}
              type="number"
              min={0}
              max={365}
              {...register('zahlungsfristTage', { valueAsNumber: true })}
            />
          )}
        </Feld>
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button variante="sekundaer" onClick={onAbbrechen}>
          Abbrechen
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {speichernText}
        </Button>
      </div>
    </form>
  )
}
