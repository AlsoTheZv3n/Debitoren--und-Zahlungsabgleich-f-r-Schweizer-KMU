import { useQueryClient } from '@tanstack/react-query'
import {
  createColumnHelper,
  rowSelectionFeature,
  tableFeatures,
  useTable,
  type RowSelectionState,
} from '@tanstack/react-table'
import { Download, Mail, Printer } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router'
import {
  mahnungErneutSenden,
  mahnungPdf,
  mahnungPerPostVersendet,
  useMahnungenFreigeben,
} from '@/shared/api/generiert/endpunkte'
import type { FreigabeErgebnisDto, MahnungDto } from '@/shared/api/generiert/modell'
import { dateiSpeichern } from '@/shared/lib/download'
import { formatDatum, formatZeitpunkt } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { Badge } from '@/shared/ui/Badge'
import { Betrag } from '@/shared/ui/Betrag'
import { Button, IconButton } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Ankreuzfeld } from '@/shared/ui/Eingabe'
import { Meldung } from '@/shared/ui/Meldung'
import { Kopfzelle, Tabelle, Zeile, Zelle } from '@/shared/ui/Tabelle'
import { pdfName, versandstand, type Ansicht } from './mahnwesen-texte'
import { VerwerfenDialog } from './VerwerfenDialog'

const funktionen = tableFeatures({ rowSelectionFeature })
const spalte = createColumnHelper<typeof funktionen, MahnungDto>()

const rechts = (titel: string) => () => <span className="block text-right">{titel}</span>

/** Neu laden, was eine Mahnung berührt: Listen, Rechnungen mit ihren Gebühren und das Dashboard. */
async function allesNeuLaden(queryClient: ReturnType<typeof useQueryClient>) {
  await Promise.all([
    invalidieren(queryClient, '/api/mahnungen'),
    invalidieren(queryClient, '/api/rechnungen'),
    invalidieren(queryClient, '/api/reporting'),
    invalidieren(queryClient, '/api/audit'),
  ])
}

/**
 * Mahnungen einer Ansicht. Vorschläge lassen sich gesammelt freigeben oder einzeln verwerfen; freigegebene
 * Schreiben ohne E-Mail werden nach dem Drucken als versendet markiert.
 */
export function MahnungenTabelle({
  ansicht,
  mahnungen,
  bearbeitbar,
}: {
  ansicht: Ansicht
  mahnungen: MahnungDto[]
  bearbeitbar: boolean
}) {
  const queryClient = useQueryClient()
  const freigeben = useMahnungenFreigeben()
  const [auswahl, setAuswahl] = useState<RowSelectionState>({})
  const [rueckfrage, setRueckfrage] = useState(false)
  const [ergebnis, setErgebnis] = useState<FreigabeErgebnisDto | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)
  const [erfolg, setErfolg] = useState<string | null>(null)
  const [zuVerwerfen, setZuVerwerfen] = useState<MahnungDto | null>(null)
  const auswaehlbar = bearbeitbar && ansicht.status === 'VORSCHLAG'

  const spalten = useMemo(() => {
    const aktion = async (text: string, schritt: () => Promise<unknown>) => {
      setFehler(null)
      setErfolg(null)
      try {
        await schritt()
        setErfolg(text)
      } catch (ausnahme) {
        setFehler(fehlertext(ausnahme))
      }
      await allesNeuLaden(queryClient)
    }

    const liste = spalte.columns([
      spalte.accessor('rechnungsnummer', {
        header: 'Rechnung',
        cell: (info) => (
          <Link
            to={`/rechnungen/${info.row.original.rechnungId}`}
            className="font-medium whitespace-nowrap text-slate-900 tabular-nums hover:text-brand-700 hover:underline focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none"
          >
            {info.getValue()}
          </Link>
        ),
      }),
      spalte.accessor('kundeName', { header: 'Kunde' }),
      spalte.accessor('bezeichnung', {
        header: 'Stufe',
        cell: (info) => (
          <>
            <span className="block">{info.getValue()}</span>
            <span className="block text-xs text-slate-500">Stufe {info.row.original.stufe}</span>
          </>
        ),
      }),
      ansicht.status === 'VORSCHLAG'
        ? spalte.accessor('faelligAm', {
            header: 'Fällig seit',
            cell: (info) => (
              <>
                <span className="block tabular-nums">{formatDatum(info.getValue())}</span>
                <span className="block text-xs text-slate-500">
                  {info.row.original.tageUeberfaellig} Tage
                </span>
              </>
            ),
          })
        : spalte.accessor('mahndatum', {
            header: 'Mahndatum',
            cell: (info) => (
              <>
                <span className="block tabular-nums">{formatDatum(info.getValue())}</span>
                <span className="block text-xs text-slate-500">
                  Frist {formatDatum(info.row.original.fristBis)}
                </span>
              </>
            ),
          }),
      spalte.accessor('gebuehr', {
        header: rechts('Gebühr'),
        cell: (info) => (
          <Betrag
            className="w-full"
            betrag={info.getValue()}
            waehrung={info.row.original.waehrung}
          />
        ),
      }),
      spalte.accessor('zuZahlen', {
        header: rechts('Betrag'),
        cell: (info) => (
          <Betrag
            className="w-full font-medium"
            betrag={info.getValue()}
            waehrung={info.row.original.waehrung}
          />
        ),
      }),
      // Bei Vorschlägen steht der Versandweg noch nicht fest; er ergibt sich erst bei der Freigabe.
      ...(ansicht.status === 'VORSCHLAG'
        ? []
        : [
            ansicht.status === 'VERWORFEN'
              ? spalte.accessor('verworfenAm', {
                  header: 'Verworfen',
                  cell: (info) => {
                    const mahnung = info.row.original
                    return (
                      <>
                        <span className="block">
                          {mahnung.verworfenGrund ?? 'Ohne Grund'}
                          {mahnung.verworfenVon && (
                            <span className="text-slate-500"> · {mahnung.verworfenVon}</span>
                          )}
                        </span>
                        {mahnung.gesperrtBis && (
                          <span className="block text-xs text-slate-500">
                            Gesperrt bis {formatDatum(mahnung.gesperrtBis)}
                          </span>
                        )}
                      </>
                    )
                  },
                })
              : spalte.display({
                  id: 'versand',
                  header: 'Versand',
                  cell: ({ row }) => {
                    const mahnung = row.original
                    const stand = versandstand(mahnung)
                    return (
                      <>
                        <Badge variante={stand.variante}>{stand.text}</Badge>
                        {mahnung.versendetAm && (
                          <span className="mt-0.5 block text-xs text-slate-500 tabular-nums">
                            {formatZeitpunkt(mahnung.versendetAm)}
                          </span>
                        )}
                        {mahnung.versandfehler && (
                          <span className="mt-0.5 block max-w-64 text-xs text-red-700">
                            {mahnung.versandfehler}
                          </span>
                        )}
                      </>
                    )
                  },
                }),
          ]),
      spalte.display({
        id: 'aktionen',
        header: () => <span className="sr-only">Aktionen</span>,
        cell: ({ row }) => {
          const mahnung = row.original
          return (
            <span className="flex items-center justify-end gap-1">
              {bearbeitbar && mahnung.status === 'VORSCHLAG' && (
                <Button
                  groesse="sm"
                  variante="dezent"
                  onClick={() => {
                    setZuVerwerfen(mahnung)
                  }}
                >
                  Verwerfen
                </Button>
              )}
              {bearbeitbar && mahnung.status === 'FREIGEGEBEN' && mahnung.versandweg === 'POST' && (
                <Button
                  groesse="sm"
                  variante="sekundaer"
                  onClick={() =>
                    void aktion(
                      `${mahnung.rechnungsnummer}: als per Post versendet markiert.`,
                      () => mahnungPerPostVersendet(mahnung.id, { version: mahnung.version }),
                    )
                  }
                >
                  <Printer aria-hidden className="size-4" />
                  Versendet
                </Button>
              )}
              {bearbeitbar && mahnung.status === 'FREIGEGEBEN' && mahnung.versandfehler && (
                <Button
                  groesse="sm"
                  variante="sekundaer"
                  onClick={() =>
                    void aktion(`${mahnung.rechnungsnummer}: erneut gesendet.`, () =>
                      mahnungErneutSenden(mahnung.id, { version: mahnung.version }),
                    )
                  }
                >
                  <Mail aria-hidden className="size-4" />
                  Erneut senden
                </Button>
              )}
              {mahnung.status !== 'VERWORFEN' && (
                <IconButton
                  variante="dezent"
                  groesse="sm"
                  aria-label={`${mahnung.status === 'VORSCHLAG' ? 'Vorschau' : 'Schreiben'} zu ${mahnung.rechnungsnummer} herunterladen`}
                  onClick={() => {
                    setFehler(null)
                    mahnungPdf(mahnung.id)
                      .then((inhalt) => {
                        dateiSpeichern(inhalt, pdfName(mahnung))
                      })
                      .catch((ausnahme: unknown) => {
                        setFehler(fehlertext(ausnahme))
                      })
                  }}
                >
                  <Download aria-hidden className="size-4" />
                </IconButton>
              )}
            </span>
          )
        },
      }),
    ])

    const auswahlspalte = spalte.display({
      id: 'auswahl',
      header: ({ table }) => (
        <Ankreuzfeld
          aria-label="Alle Vorschläge auf dieser Seite auswählen"
          checked={table.getIsAllRowsSelected()}
          unbestimmt={table.getIsSomeRowsSelected()}
          onChange={table.getToggleAllRowsSelectedHandler()}
        />
      ),
      cell: ({ row }) => (
        <Ankreuzfeld
          aria-label={`${row.original.bezeichnung} zu ${row.original.rechnungsnummer} auswählen`}
          checked={row.getIsSelected()}
          onChange={row.getToggleSelectedHandler()}
        />
      ),
    })
    return auswaehlbar ? spalte.columns([auswahlspalte, ...liste]) : liste
  }, [ansicht.status, auswaehlbar, bearbeitbar, queryClient])

  const tabelle = useTable({
    features: funktionen,
    columns: spalten,
    data: mahnungen,
    getRowId: (mahnung) => mahnung.id,
    enableRowSelection: auswaehlbar,
    state: { rowSelection: auswahl },
    onRowSelectionChange: setAuswahl,
  })
  const gewaehlt = tabelle.getSelectedRowModel().rows.map((zeile) => zeile.original)

  const sammelfreigabe = async () => {
    setFehler(null)
    setErfolg(null)
    try {
      const antwort = await freigeben.mutateAsync({
        data: {
          mahnungen: gewaehlt.map((mahnung) => ({ id: mahnung.id, version: mahnung.version })),
        },
      })
      setErgebnis(antwort)
      setAuswahl({})
    } catch (ausnahme) {
      setFehler(fehlertext(ausnahme))
    }
    setRueckfrage(false)
    await allesNeuLaden(queryClient)
  }

  const gebuehren = gewaehlt.filter((mahnung) => mahnung.gebuehr > 0).length

  return (
    <>
      {(fehler ?? erfolg ?? ergebnis) && (
        <div className="space-y-2 border-b border-slate-200 p-3">
          {fehler && <Meldung>{fehler}</Meldung>}
          {erfolg && <Meldung art="erfolg">{erfolg}</Meldung>}
          {ergebnis && ergebnis.freigegeben > 0 && (
            <Meldung art="erfolg">
              {ergebnis.freigegeben === 1
                ? 'Eine Mahnung ist freigegeben; sie geht per E-Mail oder liegt unter «Zum Versand» zum Drucken bereit.'
                : `${String(ergebnis.freigegeben)} Mahnungen sind freigegeben; sie gehen per E-Mail oder liegen unter «Zum Versand» zum Drucken bereit.`}
            </Meldung>
          )}
          {ergebnis && ergebnis.nichtFreigegeben.length > 0 && (
            <Meldung>
              <p>Nicht freigegeben:</p>
              <ul className="list-disc pl-4">
                {ergebnis.nichtFreigegeben.map((ablehnung) => (
                  <li key={ablehnung.mahnungId}>
                    {ablehnung.rechnungsnummer ? `${ablehnung.rechnungsnummer}: ` : ''}
                    {ablehnung.grund}
                  </li>
                ))}
              </ul>
            </Meldung>
          )}
        </div>
      )}
      {gewaehlt.length > 0 && (
        <div
          role="region"
          aria-label="Sammelaktionen"
          className="flex items-center gap-3 border-b border-slate-200 bg-brand-50 px-3 py-2 text-sm"
        >
          <span className="font-medium text-brand-700">
            {gewaehlt.length === 1
              ? 'Ein Vorschlag gewählt'
              : `${String(gewaehlt.length)} Vorschläge gewählt`}
          </span>
          <Button
            groesse="sm"
            onClick={() => {
              setRueckfrage(true)
            }}
          >
            Freigeben
          </Button>
          <Button
            groesse="sm"
            variante="dezent"
            onClick={() => {
              setAuswahl({})
            }}
          >
            Auswahl aufheben
          </Button>
        </div>
      )}
      <Tabelle>
        <thead>
          {tabelle.getHeaderGroups().map((gruppe) => (
            <tr key={gruppe.id}>
              {gruppe.headers.map((kopf) => (
                <Kopfzelle key={kopf.id} className={kopf.id === 'auswahl' ? 'w-10' : undefined}>
                  <tabelle.FlexRender header={kopf} />
                </Kopfzelle>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {tabelle.getRowModel().rows.map((zeile) => (
            <Zeile
              key={zeile.id}
              data-ausgewaehlt={zeile.getIsSelected() || undefined}
              className="data-ausgewaehlt:bg-brand-50/60"
            >
              {zeile.getAllCells().map((zelle) => (
                <Zelle key={zelle.id}>
                  <tabelle.FlexRender cell={zelle} />
                </Zelle>
              ))}
            </Zeile>
          ))}
        </tbody>
      </Tabelle>

      <Dialog
        offen={rueckfrage}
        titel={
          gewaehlt.length === 1
            ? 'Einen Vorschlag freigeben?'
            : `${String(gewaehlt.length)} Vorschläge freigeben?`
        }
        onSchliessen={() => {
          if (!freigeben.isPending) {
            setRueckfrage(false)
          }
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              disabled={freigeben.isPending}
              onClick={() => {
                setRueckfrage(false)
              }}
            >
              Abbrechen
            </Button>
            <Button disabled={freigeben.isPending} onClick={() => void sammelfreigabe()}>
              {freigeben.isPending ? 'Wird freigegeben…' : 'Freigeben'}
            </Button>
          </>
        }
      >
        <p>
          Jedes Schreiben geht per E-Mail an den Kunden; ohne E-Mail-Adresse liegt es unter «Zum
          Versand» zum Drucken bereit.
        </p>
        {gebuehren > 0 && (
          <p className="mt-2">
            {gebuehren === 1
              ? 'Eine Mahnung enthält eine Gebühr; sie wird der Rechnung belastet.'
              : `${String(gebuehren)} Mahnungen enthalten eine Gebühr; sie wird der Rechnung belastet.`}
          </p>
        )}
      </Dialog>

      {zuVerwerfen && (
        <VerwerfenDialog
          mahnung={zuVerwerfen}
          onSchliessen={() => {
            setZuVerwerfen(null)
          }}
          onVerworfen={async () => {
            setErfolg(`${zuVerwerfen.rechnungsnummer}: Vorschlag verworfen.`)
            setZuVerwerfen(null)
            await allesNeuLaden(queryClient)
          }}
        />
      )}
    </>
  )
}
