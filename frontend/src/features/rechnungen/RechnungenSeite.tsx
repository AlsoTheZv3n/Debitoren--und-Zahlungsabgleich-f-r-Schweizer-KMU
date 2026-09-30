import { keepPreviousData, useQueryClient } from '@tanstack/react-query'
import {
  createColumnHelper,
  rowSelectionFeature,
  tableFeatures,
  useTable,
  type RowSelectionState,
} from '@tanstack/react-table'
import { FileText, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { rechnungFreigeben, useRechnungenSuchen } from '@/shared/api/generiert/endpunkte'
import {
  RechnungenSuchenSortierung,
  RechnungenSuchenStatus,
  type RechnungZeileDto,
} from '@/shared/api/generiert/modell'
import { formatDatum } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { Betrag } from '@/shared/ui/Betrag'
import { Button, LinkButton } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Ankreuzfeld, Auswahl, Eingabe } from '@/shared/ui/Eingabe'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { RechnungsstatusBadge } from '@/shared/ui/RechnungsstatusBadge'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Seitenwahl } from '@/shared/ui/Seitenwahl'
import { Kopfzelle, Tabelle, Zeile, Zelle } from '@/shared/ui/Tabelle'

const STATUS_TEXT: Record<RechnungenSuchenStatus, string> = {
  ENTWURF: 'Entwurf',
  OFFEN: 'Offen',
  TEILBEZAHLT: 'Teilbezahlt',
  BEZAHLT: 'Bezahlt',
  STORNIERT: 'Storniert',
}

const SORTIERUNG_TEXT: Record<RechnungenSuchenSortierung, string> = {
  DATUM_ABSTEIGEND: 'Neueste zuerst',
  DATUM_AUFSTEIGEND: 'Älteste zuerst',
  FAELLIGKEIT_AUFSTEIGEND: 'Nach Fälligkeit',
  NUMMER_ABSTEIGEND: 'Nach Nummer',
  TOTAL_ABSTEIGEND: 'Höchster Betrag zuerst',
}

const alsStatus = (wert: string | null) =>
  wert && wert in RechnungenSuchenStatus ? (wert as RechnungenSuchenStatus) : undefined

const alsSortierung = (wert: string | null) =>
  wert && wert in RechnungenSuchenSortierung
    ? (wert as RechnungenSuchenSortierung)
    : RechnungenSuchenSortierung.DATUM_ABSTEIGEND

const funktionen = tableFeatures({ rowSelectionFeature })
const spalte = createColumnHelper<typeof funktionen, RechnungZeileDto>()

const bezeichnung = (rechnung: RechnungZeileDto) =>
  `${rechnung.rechnungsnummer ?? 'Entwurf'} an ${rechnung.kundeName}`

/** Link zur Rechnung; Entwürfe öffnen sich im Editor, alle anderen in der Ansicht. */
function RechnungLink({ rechnung }: { rechnung: RechnungZeileDto }) {
  return (
    <Link
      to={`/rechnungen/${rechnung.id}`}
      className="font-medium text-slate-900 tabular-nums hover:text-brand-700 hover:underline focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none"
    >
      {rechnung.rechnungsnummer ?? <span className="text-slate-500 italic">Entwurf</span>}
    </Link>
  )
}

const spalten = spalte.columns([
  spalte.display({
    id: 'auswahl',
    header: ({ table }) => (
      <Ankreuzfeld
        aria-label="Alle Entwürfe auf dieser Seite auswählen"
        checked={table.getIsAllRowsSelected()}
        unbestimmt={table.getIsSomeRowsSelected()}
        disabled={!table.getRowModel().rows.some((zeile) => zeile.getCanSelect())}
        onChange={table.getToggleAllRowsSelectedHandler()}
      />
    ),
    cell: ({ row }) =>
      row.getCanSelect() ? (
        <Ankreuzfeld
          aria-label={`${bezeichnung(row.original)} auswählen`}
          checked={row.getIsSelected()}
          onChange={row.getToggleSelectedHandler()}
          onClick={(ereignis) => {
            ereignis.stopPropagation()
          }}
        />
      ) : null,
  }),
  spalte.accessor('rechnungsnummer', {
    header: 'Nummer',
    cell: (info) => <RechnungLink rechnung={info.row.original} />,
  }),
  spalte.accessor('kundeName', { header: 'Kunde' }),
  spalte.accessor('rechnungsdatum', {
    header: 'Datum',
    cell: (info) => <span className="tabular-nums">{formatDatum(info.getValue())}</span>,
  }),
  spalte.accessor('faelligAm', {
    header: 'Fällig',
    cell: (info) => <span className="tabular-nums">{formatDatum(info.getValue())}</span>,
  }),
  spalte.accessor('status', {
    header: 'Status',
    cell: (info) => (
      <RechnungsstatusBadge
        status={info.getValue()}
        ueberfaellig={info.row.original.ueberfaellig}
      />
    ),
  }),
  spalte.accessor('total', {
    header: () => <span className="block text-right">Total</span>,
    cell: (info) => (
      <Betrag className="w-full" betrag={info.getValue()} waehrung={info.row.original.waehrung} />
    ),
  }),
  spalte.accessor('offen', {
    header: () => <span className="block text-right">Offen</span>,
    cell: (info) => (
      <Betrag className="w-full" betrag={info.getValue()} waehrung={info.row.original.waehrung} />
    ),
  }),
])

/** Leser wählen nichts aus; für sie fehlt die erste Spalte ganz. */
const spaltenOhneAuswahl = spalten.slice(1)

export function RechnungenSeite() {
  const [parameter, setParameter] = useSearchParams()
  const location = useLocation()
  const { hatRolle } = useRollen()
  const suche = parameter.get('suche') ?? ''
  const status = alsStatus(parameter.get('status'))
  const nurOffene = parameter.get('offen') === 'ja'
  const ueberfaellig = parameter.get('ueberfaellig') === 'ja'
  const sortierung = alsSortierung(parameter.get('sortierung'))
  const seite = Number(parameter.get('seite') ?? '0')
  const [eingabe, setEingabe] = useState(suche)
  const entprellt = useEntprellt(eingabe)

  /** Setzt einen Filter; jede Filteränderung beginnt wieder auf der ersten Seite. */
  const filtern = (name: string, wert: string | undefined) => {
    setParameter(
      (alt) => {
        const neu = new URLSearchParams(alt)
        if (wert) {
          neu.set(name, wert)
        } else {
          neu.delete(name)
        }
        neu.delete('seite')
        return neu
      },
      { replace: true },
    )
  }

  useEffect(() => {
    if (entprellt.trim() !== suche) {
      setParameter(
        (alt) => {
          const neu = new URLSearchParams(alt)
          if (entprellt.trim()) {
            neu.set('suche', entprellt.trim())
          } else {
            neu.delete('suche')
          }
          neu.delete('seite')
          return neu
        },
        { replace: true },
      )
    }
  }, [entprellt, suche, setParameter])

  const abfrage = useRechnungenSuchen(
    {
      suche: suche || undefined,
      status,
      offen: nurOffene || undefined,
      ueberfaellig: ueberfaellig || undefined,
      sortierung,
      seite,
      groesse: 25,
    },
    { query: { placeholderData: keepPreviousData } },
  )
  const gefiltert = suche !== '' || status !== undefined || nurOffene || ueberfaellig

  return (
    <>
      <Seitenkopf
        titel="Rechnungen"
        beschreibung="QR-Rechnungen erstellen, freigeben und stornieren."
        aktionen={
          hatRolle('BUCHHALTUNG') && (
            <LinkButton to="/rechnungen/neu">
              <Plus aria-hidden className="size-4" />
              Neue Rechnung
            </LinkButton>
          )
        }
      />
      <section aria-label="Rechnungsliste" className="rounded-lg border border-slate-200 bg-white">
        <div
          role="search"
          className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3"
        >
          <label className="relative block w-full max-w-xs">
            <span className="sr-only">Rechnungen suchen</span>
            <Search
              aria-hidden
              className="pointer-events-none absolute top-2.5 left-3 size-4 text-slate-400"
            />
            <Eingabe
              type="search"
              value={eingabe}
              placeholder="Nummer oder Kunde"
              className="pl-9"
              onChange={(ereignis) => {
                setEingabe(ereignis.target.value)
              }}
            />
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            Status
            <Auswahl
              className="w-40"
              value={status ?? ''}
              onChange={(ereignis) => {
                filtern('status', ereignis.target.value || undefined)
              }}
            >
              <option value="">Alle</option>
              {Object.values(RechnungenSuchenStatus).map((wert) => (
                <option key={wert} value={wert}>
                  {STATUS_TEXT[wert]}
                </option>
              ))}
            </Auswahl>
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <Ankreuzfeld
              checked={nurOffene}
              onChange={(ereignis) => {
                filtern('offen', ereignis.target.checked ? 'ja' : undefined)
              }}
            />
            Nur offene
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <Ankreuzfeld
              checked={ueberfaellig}
              onChange={(ereignis) => {
                filtern('ueberfaellig', ereignis.target.checked ? 'ja' : undefined)
              }}
            />
            Nur überfällige
          </label>
          <label className="ml-auto flex items-center gap-2 text-sm text-slate-700">
            Sortierung
            <Auswahl
              className="w-52"
              value={sortierung}
              onChange={(ereignis) => {
                filtern(
                  'sortierung',
                  ereignis.target.value === RechnungenSuchenSortierung.DATUM_ABSTEIGEND
                    ? undefined
                    : ereignis.target.value,
                )
              }}
            >
              {Object.values(RechnungenSuchenSortierung).map((wert) => (
                <option key={wert} value={wert}>
                  {SORTIERUNG_TEXT[wert]}
                </option>
              ))}
            </Auswahl>
          </label>
        </div>
        {abfrage.isPending ? (
          <Ladeanzeige className="px-3" />
        ) : abfrage.isError ? (
          <Meldung className="m-3">{fehlertext(abfrage.error)}</Meldung>
        ) : abfrage.data.inhalt.length === 0 ? (
          <LeererZustand
            symbol={FileText}
            titel={gefiltert ? 'Keine Treffer' : 'Noch keine Rechnungen'}
          >
            {gefiltert
              ? 'Keine Rechnung passt zu diesen Filtern.'
              : 'Erstellen Sie die erste Rechnung.'}
          </LeererZustand>
        ) : (
          <>
            {/* Neue Filter oder eine andere Seite beginnen mit leerer Auswahl. */}
            <RechnungenTabelle
              key={location.search}
              rechnungen={abfrage.data.inhalt}
              auswaehlbar={hatRolle('BUCHHALTUNG')}
            />
            <Seitenwahl
              seite={abfrage.data.seite}
              anzahlSeiten={abfrage.data.anzahlSeiten}
              anzahlElemente={abfrage.data.anzahlElemente}
              onSeite={(neueSeite) => {
                setParameter((alt) => {
                  const neu = new URLSearchParams(alt)
                  neu.set('seite', String(neueSeite))
                  return neu
                })
              }}
            />
          </>
        )}
      </section>
    </>
  )
}

interface Freigabeergebnis {
  freigegeben: number
  fehler: string[]
}

function RechnungenTabelle({
  rechnungen,
  auswaehlbar,
}: {
  rechnungen: RechnungZeileDto[]
  auswaehlbar: boolean
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [auswahl, setAuswahl] = useState<RowSelectionState>({})
  const [rueckfrage, setRueckfrage] = useState(false)
  const [laeuft, setLaeuft] = useState(false)
  const [ergebnis, setErgebnis] = useState<Freigabeergebnis | null>(null)

  const tabelle = useTable({
    features: funktionen,
    columns: auswaehlbar ? spalten : spaltenOhneAuswahl,
    data: rechnungen,
    getRowId: (rechnung) => rechnung.id,
    enableRowSelection: (zeile) => auswaehlbar && zeile.original.status === 'ENTWURF',
    state: { rowSelection: auswahl },
    onRowSelectionChange: setAuswahl,
  })
  const gewaehlt = tabelle.getSelectedRowModel().rows.map((zeile) => zeile.original)

  /**
   * Gibt die gewählten Entwürfe nacheinander frei, damit die Nummern in der gewählten Reihenfolge
   * entstehen; ein Fehler bei einem Entwurf hält die übrigen nicht auf.
   */
  const sammelfreigabe = async () => {
    setLaeuft(true)
    const neu: Freigabeergebnis = { freigegeben: 0, fehler: [] }
    for (const rechnung of gewaehlt) {
      try {
        await rechnungFreigeben(rechnung.id, { version: rechnung.version })
        neu.freigegeben += 1
      } catch (fehler) {
        neu.fehler.push(`${bezeichnung(rechnung)}: ${fehlertext(fehler)}`)
      }
    }
    setLaeuft(false)
    setRueckfrage(false)
    setAuswahl({})
    setErgebnis(neu)
    await invalidieren(queryClient, '/api/rechnungen')
  }

  return (
    <>
      {ergebnis && (
        <div className="space-y-2 border-b border-slate-200 p-3">
          {ergebnis.freigegeben > 0 && (
            <Meldung art="erfolg">
              {ergebnis.freigegeben === 1
                ? 'Eine Rechnung ist freigegeben.'
                : `${ergebnis.freigegeben} Rechnungen sind freigegeben.`}
            </Meldung>
          )}
          {ergebnis.fehler.length > 0 && (
            <Meldung>
              <p>Nicht freigegeben:</p>
              <ul className="list-disc pl-4">
                {ergebnis.fehler.map((text) => (
                  <li key={text}>{text}</li>
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
            {gewaehlt.length === 1 ? 'Ein Entwurf gewählt' : `${gewaehlt.length} Entwürfe gewählt`}
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
              className="cursor-pointer data-ausgewaehlt:bg-brand-50/60"
              onClick={() => {
                void navigate(`/rechnungen/${zeile.original.id}`)
              }}
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
            ? 'Einen Entwurf freigeben?'
            : `${gewaehlt.length} Entwürfe freigeben?`
        }
        onSchliessen={() => {
          if (!laeuft) {
            setRueckfrage(false)
          }
        }}
        aktionen={
          <>
            <Button
              variante="sekundaer"
              disabled={laeuft}
              onClick={() => {
                setRueckfrage(false)
              }}
            >
              Abbrechen
            </Button>
            <Button disabled={laeuft} onClick={() => void sammelfreigabe()}>
              {laeuft ? 'Wird freigegeben…' : 'Freigeben'}
            </Button>
          </>
        }
      >
        Jede Rechnung erhält eine Nummer und eine Zahlungsreferenz; danach lässt sie sich nur noch
        stornieren.
      </Dialog>
    </>
  )
}
