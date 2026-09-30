import { keepPreviousData, useQueryClient } from '@tanstack/react-query'
import { createColumnHelper, tableFeatures, useTable } from '@tanstack/react-table'
import { Download, LoaderCircle, Upload } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import {
  useZahlungsdateiHochladen,
  useZahlungsdateienAuflisten,
  zahlungsdateiOriginal,
} from '@/shared/api/generiert/endpunkte'
import type { ZahlungsdateiDto } from '@/shared/api/generiert/modell'
import { dateiSpeichern } from '@/shared/lib/download'
import { formatZeitpunkt } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { IconButton } from '@/shared/ui/Button'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Seitenwahl } from '@/shared/ui/Seitenwahl'
import { Kopfzelle, Tabelle, Zeile, Zelle } from '@/shared/ui/Tabelle'
import { Dateiablage } from './Dateiablage'

interface Ergebnis {
  dateiname: string
  erfolgreich: boolean
  text: string
}

const funktionen = tableFeatures({})
const spalte = createColumnHelper<typeof funktionen, ZahlungsdateiDto>()

const zahl = (titel: string) => () => <span className="block text-right">{titel}</span>
const rechts = (wert: number) => <span className="block text-right tabular-nums">{wert}</span>

function OriginalLaden({ datei }: { datei: ZahlungsdateiDto }) {
  const [fehler, setFehler] = useState<string | null>(null)
  return (
    <IconButton
      variante="dezent"
      groesse="sm"
      aria-label={`Original ${datei.dateiname} herunterladen`}
      title={fehler ?? undefined}
      onClick={() => {
        zahlungsdateiOriginal(datei.id)
          .then((inhalt) => {
            dateiSpeichern(inhalt, datei.dateiname)
          })
          .catch((ausnahme: unknown) => {
            setFehler(fehlertext(ausnahme))
          })
      }}
    >
      <Download aria-hidden className="size-4" />
    </IconButton>
  )
}

const spalten = spalte.columns([
  spalte.accessor('importiertAm', {
    header: 'Importiert',
    cell: (info) => (
      <>
        <span className="block tabular-nums">{formatZeitpunkt(info.getValue())}</span>
        <span className="block text-xs text-slate-500">{info.row.original.importiertVon}</span>
      </>
    ),
  }),
  spalte.accessor('dateiname', {
    header: 'Datei',
    cell: (info) => (
      <span className="flex items-center gap-1">
        <span className="min-w-0 truncate">{info.getValue()}</span>
        <OriginalLaden datei={info.row.original} />
      </span>
    ),
  }),
  spalte.accessor('anzahlBuchungen', {
    header: zahl('Buchungen'),
    cell: (info) => rechts(info.getValue()),
  }),
  spalte.accessor('anzahlNeu', { header: zahl('Neu'), cell: (info) => rechts(info.getValue()) }),
  spalte.accessor('anzahlUebersprungen', {
    header: zahl('Bekannt'),
    cell: (info) => rechts(info.getValue()),
  }),
  spalte.accessor('zugeordnet', {
    header: zahl('Zugeordnet'),
    cell: (info) => rechts(info.getValue()),
  }),
  spalte.accessor('inKlaerung', {
    header: zahl('Klärung'),
    cell: (info) =>
      info.getValue() > 0 ? (
        <Link
          to="/zahlungen/klaerung"
          className="block text-right font-medium text-violet-700 tabular-nums hover:underline"
        >
          {info.getValue()}
        </Link>
      ) : (
        rechts(0)
      ),
  }),
  spalte.accessor('unbearbeitet', {
    header: 'Abgleich',
    cell: (info) =>
      info.getValue() > 0 ? (
        <span className="inline-flex items-center gap-1.5 text-slate-600">
          <LoaderCircle aria-hidden className="size-3.5 animate-spin" />
          läuft
        </span>
      ) : (
        <span className="text-slate-600">fertig</span>
      ),
  }),
])

export function ImportSeite() {
  const queryClient = useQueryClient()
  const { hatRolle } = useRollen()
  const [seite, setSeite] = useState(0)
  const [ergebnisse, setErgebnisse] = useState<Ergebnis[]>([])
  const hochladen = useZahlungsdateiHochladen()
  const protokoll = useZahlungsdateienAuflisten(
    { seite, groesse: 25 },
    {
      query: {
        placeholderData: keepPreviousData,
        // Solange der Abgleich läuft, bleibt der Stand frisch.
        refetchInterval: (abfrage) =>
          abfrage.state.data?.inhalt.some((datei) => datei.unbearbeitet > 0) ? 1500 : false,
      },
    },
  )

  // Ist der Abgleich fertig, ändern sich Klärungsliste und Rechnungen.
  const liefAbgleich = useRef(false)
  const laeuftAbgleich = protokoll.data?.inhalt.some((datei) => datei.unbearbeitet > 0) ?? false
  useEffect(() => {
    if (liefAbgleich.current && !laeuftAbgleich) {
      void invalidieren(queryClient, '/api/zahlungen')
      void invalidieren(queryClient, '/api/rechnungen')
    }
    liefAbgleich.current = laeuftAbgleich
  }, [laeuftAbgleich, queryClient])

  const importieren = async (dateien: File[]) => {
    const neu: Ergebnis[] = []
    for (const datei of dateien) {
      try {
        const ergebnis = await hochladen.mutateAsync({ data: { datei } })
        const teile = [
          `${ergebnis.anzahlNeu} neu`,
          `${ergebnis.anzahlUebersprungen} bereits bekannt`,
          ...(ergebnis.anzahlIgnoriert > 0 ? [`${ergebnis.anzahlIgnoriert} ignoriert`] : []),
        ]
        neu.push({
          dateiname: ergebnis.dateiname,
          erfolgreich: true,
          text: `${ergebnis.anzahlBuchungen} ${ergebnis.anzahlBuchungen === 1 ? 'Gutschrift' : 'Gutschriften'}: ${teile.join(', ')}.`,
        })
      } catch (fehler) {
        neu.push({ dateiname: datei.name, erfolgreich: false, text: fehlertext(fehler) })
      }
    }
    setErgebnisse(neu)
    setSeite(0)
    await invalidieren(queryClient, '/api/zahlungsdateien')
    await invalidieren(queryClient, '/api/zahlungen')
  }

  return (
    <>
      <Seitenkopf
        titel="Zahlungsimport"
        beschreibung="camt.054-Dateien der Bank hochladen; der Abgleich ordnet die Zahlungen danach zu."
      />
      <div className="space-y-6">
        {hatRolle('BUCHHALTUNG') && (
          <section aria-label="Dateien hochladen" className="space-y-3">
            <Dateiablage
              onDateien={(dateien) => void importieren(dateien)}
              beschaeftigt={hochladen.isPending}
            />
            {ergebnisse.map((ergebnis) => (
              <Meldung key={ergebnis.dateiname} art={ergebnis.erfolgreich ? 'erfolg' : 'fehler'}>
                <strong className="font-medium">{ergebnis.dateiname}:</strong> {ergebnis.text}
              </Meldung>
            ))}
          </section>
        )}

        <section
          aria-labelledby="protokoll"
          className="rounded-lg border border-slate-200 bg-white"
        >
          <h2 id="protokoll" className="p-4 pb-2 text-sm font-semibold text-slate-900">
            Importprotokoll
          </h2>
          {protokoll.isPending ? (
            <Ladeanzeige className="px-4" />
          ) : protokoll.isError ? (
            <Meldung className="m-4">{fehlertext(protokoll.error)}</Meldung>
          ) : protokoll.data.inhalt.length === 0 ? (
            <div className="p-4 pt-0">
              <LeererZustand symbol={Upload} titel="Noch keine Zahlungsdateien">
                Hier erscheint jede importierte Datei mit dem Stand ihrer Zahlungen.
              </LeererZustand>
            </div>
          ) : (
            <>
              <ProtokollTabelle dateien={protokoll.data.inhalt} />
              <Seitenwahl
                seite={protokoll.data.seite}
                anzahlSeiten={protokoll.data.anzahlSeiten}
                anzahlElemente={protokoll.data.anzahlElemente}
                onSeite={setSeite}
              />
            </>
          )}
        </section>
      </div>
    </>
  )
}

function ProtokollTabelle({ dateien }: { dateien: ZahlungsdateiDto[] }) {
  const tabelle = useTable({
    features: funktionen,
    columns: spalten,
    data: dateien,
    getRowId: (datei) => datei.id,
  })
  return (
    <Tabelle>
      <thead>
        {tabelle.getHeaderGroups().map((gruppe) => (
          <tr key={gruppe.id}>
            {gruppe.headers.map((kopf) => (
              <Kopfzelle key={kopf.id}>
                <tabelle.FlexRender header={kopf} />
              </Kopfzelle>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {tabelle.getRowModel().rows.map((zeile) => (
          <Zeile key={zeile.id}>
            {zeile.getAllCells().map((zelle) => (
              <Zelle key={zelle.id}>
                <tabelle.FlexRender cell={zelle} />
              </Zelle>
            ))}
          </Zeile>
        ))}
      </tbody>
    </Tabelle>
  )
}
