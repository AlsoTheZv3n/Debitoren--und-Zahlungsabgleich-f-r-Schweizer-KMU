import { useQueryClient } from '@tanstack/react-query'
import { Ban, Download } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  getRechnungLesenQueryKey,
  rechnungPdf,
  useRechnungStornieren,
} from '@/shared/api/generiert/endpunkte'
import type { RechnungDto } from '@/shared/api/generiert/modell'
import { rechnungStornierenBodyGrundMax } from '@/shared/api/generiert/zod/rechnungen/rechnungen.zod'
import { dateiSpeichern } from '@/shared/lib/download'
import { formatDatum, formatMenge, formatProzent } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { Betrag } from '@/shared/ui/Betrag'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Textbereich } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Meldung } from '@/shared/ui/Meldung'
import { RechnungsstatusBadge } from '@/shared/ui/RechnungsstatusBadge'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Kopfzelle, Tabelle, Zelle } from '@/shared/ui/Tabelle'
import { SummenBlock } from './SummenBlock'
import { Zahlungseingaenge } from './Zahlungseingaenge'
import { GespeicherterZahlteil } from './ZahlteilBild'

/** Eine freigegebene Rechnung, wie sie beim Kunden liegt: nur lesen, PDF laden, stornieren. */
export function RechnungAnsicht({
  rechnung,
  meldung,
}: {
  rechnung: RechnungDto
  meldung?: string
}) {
  const { hatRolle } = useRollen()
  const [fehler, setFehler] = useState<string | null>(null)
  const [pdfLaedt, setPdfLaedt] = useState(false)
  const [stornoOffen, setStornoOffen] = useState(false)
  const nummer = rechnung.rechnungsnummer
  const empfaenger = rechnung.empfaenger
  const strasse = [empfaenger.strasse, empfaenger.hausnummer].filter(Boolean).join(' ')
  const nimmtZahlungenAn = rechnung.status === 'OFFEN' || rechnung.status === 'TEILBEZAHLT'

  const pdfHerunterladen = async () => {
    setFehler(null)
    setPdfLaedt(true)
    try {
      dateiSpeichern(await rechnungPdf(rechnung.id), `${nummer ?? 'Rechnung'}.pdf`)
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    } finally {
      setPdfLaedt(false)
    }
  }

  return (
    <>
      <Seitenkopf
        titel={nummer ? `Rechnung ${nummer}` : 'Rechnungsentwurf'}
        beschreibung={
          <span className="flex items-center gap-2">
            <RechnungsstatusBadge status={rechnung.status} ueberfaellig={rechnung.ueberfaellig} />
            <Link
              to={`/kunden/${rechnung.kundeId}`}
              className="hover:text-brand-700 hover:underline"
            >
              {empfaenger.name}
            </Link>
          </span>
        }
        aktionen={
          <>
            {rechnung.status !== 'ENTWURF' && (
              <Button
                variante="sekundaer"
                disabled={pdfLaedt}
                onClick={() => void pdfHerunterladen()}
              >
                <Download aria-hidden className="size-4" />
                {pdfLaedt ? 'PDF wird geladen…' : 'PDF herunterladen'}
              </Button>
            )}
            {rechnung.status === 'OFFEN' && hatRolle('BUCHHALTUNG') && (
              <Button
                variante="sekundaer"
                onClick={() => {
                  setStornoOffen(true)
                }}
              >
                <Ban aria-hidden className="size-4" />
                Stornieren
              </Button>
            )}
          </>
        }
      />

      <div className="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="space-y-6">
          {fehler && <Meldung>{fehler}</Meldung>}
          {meldung && !fehler && <Meldung art="erfolg">{meldung}</Meldung>}
          {rechnung.status === 'STORNIERT' && (
            <Meldung art="hinweis">
              Storniert
              {rechnung.storniertAm && ` am ${formatDatum(rechnung.storniertAm)}`}
              {rechnung.stornoGrund && `: ${rechnung.stornoGrund}`}
            </Meldung>
          )}

          <section
            aria-labelledby="angaben"
            className="grid gap-6 rounded-lg border border-slate-200 bg-white p-4 sm:grid-cols-2"
          >
            <h2 id="angaben" className="sr-only">
              Angaben
            </h2>
            <address className="text-sm not-italic">
              <span className="block font-medium">{empfaenger.name}</span>
              {strasse && <span className="block">{strasse}</span>}
              <span className="block">
                {empfaenger.land === 'CH' ? '' : `${empfaenger.land}-`}
                {empfaenger.plz} {empfaenger.ort}
              </span>
            </address>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm">
              <dt className="text-slate-500">Rechnungsdatum</dt>
              <dd>{formatDatum(rechnung.rechnungsdatum)}</dd>
              <dt className="text-slate-500">Fällig am</dt>
              <dd>
                {formatDatum(rechnung.faelligAm)}
                <span className="text-slate-500"> ({rechnung.zahlungsfristTage} Tage)</span>
              </dd>
              {rechnung.freigegebenAm && (
                <>
                  <dt className="text-slate-500">Freigegeben am</dt>
                  <dd>{formatDatum(rechnung.freigegebenAm)}</dd>
                </>
              )}
              {rechnung.referenz && (
                <>
                  <dt className="text-slate-500">
                    {rechnung.referenzart === 'SCOR' ? 'SCOR-Referenz' : 'QR-Referenz'}
                  </dt>
                  <dd className="font-mono text-xs leading-5">{rechnung.referenz}</dd>
                </>
              )}
              {nimmtZahlungenAn && (
                <>
                  <dt className="text-slate-500">Offen</dt>
                  <dd>
                    <Betrag
                      betrag={rechnung.offen}
                      waehrung={rechnung.waehrung}
                      className="font-medium"
                    />
                  </dd>
                </>
              )}
            </dl>
          </section>

          <section
            aria-labelledby="positionen"
            className="rounded-lg border border-slate-200 bg-white"
          >
            <h2 id="positionen" className="p-4 pb-2 text-sm font-semibold text-slate-900">
              Positionen
            </h2>
            <Tabelle>
              <thead>
                <tr>
                  <Kopfzelle className="w-8">#</Kopfzelle>
                  <Kopfzelle>Beschreibung</Kopfzelle>
                  <Kopfzelle className="text-right">Menge</Kopfzelle>
                  <Kopfzelle className="text-right">Einzelpreis</Kopfzelle>
                  <Kopfzelle className="text-right">MWST</Kopfzelle>
                  <Kopfzelle className="text-right">Betrag</Kopfzelle>
                </tr>
              </thead>
              <tbody>
                {rechnung.positionen.map((position) => (
                  <tr key={position.position}>
                    <Zelle className="text-slate-500 tabular-nums">{position.position}</Zelle>
                    <Zelle>{position.beschreibung}</Zelle>
                    <Zelle className="text-right whitespace-nowrap tabular-nums">
                      {formatMenge(position.menge)} {position.einheit}
                    </Zelle>
                    <Zelle className="text-right">
                      <Betrag betrag={position.einzelpreis} waehrung={rechnung.waehrung} />
                    </Zelle>
                    <Zelle className="text-right whitespace-nowrap tabular-nums">
                      {formatProzent(position.mwstProzent)}
                    </Zelle>
                    <Zelle className="text-right">
                      <Betrag betrag={position.betrag} waehrung={rechnung.waehrung} />
                    </Zelle>
                  </tr>
                ))}
              </tbody>
            </Tabelle>
            <div className="flex justify-end p-4">
              <SummenBlock
                summen={rechnung.summen}
                waehrung={rechnung.waehrung}
                className="w-full max-w-sm"
              />
            </div>
          </section>

          {rechnung.status !== 'ENTWURF' && <Zahlungseingaenge rechnung={rechnung} />}

          {rechnung.bemerkung && (
            <section
              aria-labelledby="bemerkung"
              className="rounded-lg border border-slate-200 bg-white p-4 text-sm"
            >
              <h2 id="bemerkung" className="mb-1 font-semibold text-slate-900">
                Bemerkung
              </h2>
              <p className="whitespace-pre-line text-slate-700">{rechnung.bemerkung}</p>
            </section>
          )}
        </div>

        {rechnung.status !== 'STORNIERT' && (
          <aside
            aria-labelledby="zahlteil"
            className="space-y-3 rounded-lg border border-slate-200 bg-white p-4"
          >
            <h2 id="zahlteil" className="text-sm font-semibold text-slate-900">
              QR-Zahlteil
            </h2>
            <GespeicherterZahlteil
              id={rechnung.id}
              beschriftung={`QR-Zahlteil der Rechnung ${nummer ?? ''}`}
            />
          </aside>
        )}
      </div>

      {stornoOffen && (
        <StornoDialog
          rechnung={rechnung}
          onSchliessen={() => {
            setStornoOffen(false)
          }}
        />
      )}
    </>
  )
}

function StornoDialog({
  rechnung,
  onSchliessen,
}: {
  rechnung: RechnungDto
  onSchliessen: () => void
}) {
  const queryClient = useQueryClient()
  const stornieren = useRechnungStornieren()
  const [grund, setGrund] = useState('')
  const [fehler, setFehler] = useState<string | null>(null)

  const bestaetigen = async () => {
    if (!grund.trim()) {
      setFehler('Bitte einen Grund angeben.')
      return
    }
    try {
      const storniert = await stornieren.mutateAsync({
        id: rechnung.id,
        data: { grund: grund.trim(), version: rechnung.version },
      })
      queryClient.setQueryData(getRechnungLesenQueryKey(rechnung.id), storniert)
      await invalidieren(queryClient, '/api/rechnungen')
      onSchliessen()
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    }
  }

  return (
    <Dialog
      offen
      titel={`Rechnung ${rechnung.rechnungsnummer ?? ''} stornieren?`}
      onSchliessen={onSchliessen}
      aktionen={
        <>
          <Button variante="sekundaer" onClick={onSchliessen}>
            Abbrechen
          </Button>
          <Button
            className="bg-red-700 hover:bg-red-800"
            disabled={stornieren.isPending}
            onClick={() => void bestaetigen()}
          >
            Stornieren
          </Button>
        </>
      }
    >
      <p className="mb-3">
        Die Rechnung bleibt mit Nummer und Referenz erhalten; eingehende Zahlungen dazu landen in
        der Klärungsliste.
      </p>
      <Feld label="Grund" fehler={fehler ?? undefined}>
        {(steuerung) => (
          <Textbereich
            {...steuerung}
            rows={2}
            maxLength={rechnungStornierenBodyGrundMax}
            value={grund}
            onChange={(ereignis) => {
              setGrund(ereignis.target.value)
            }}
          />
        )}
      </Feld>
    </Dialog>
  )
}
