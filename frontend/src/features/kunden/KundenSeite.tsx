import { keepPreviousData } from '@tanstack/react-query'
import { createColumnHelper, tableFeatures, useTable } from '@tanstack/react-table'
import { Plus, Search, Users } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router'
import { useKundenSuchen } from '@/shared/api/generiert/endpunkte'
import type { KundeDto } from '@/shared/api/generiert/modell'
import { fehlertext } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { LinkButton } from '@/shared/ui/Button'
import { Eingabe } from '@/shared/ui/Eingabe'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'
import { Seitenwahl } from '@/shared/ui/Seitenwahl'
import { Kopfzelle, Tabelle, Zeile, Zelle } from '@/shared/ui/Tabelle'

const funktionen = tableFeatures({})
const spalte = createColumnHelper<typeof funktionen, KundeDto>()

/** Link zum Kunden; behält Suche und Seite der Liste. */
function KundenLink({ kunde }: { kunde: KundeDto }) {
  const location = useLocation()
  return (
    <Link
      to={{ pathname: kunde.id, search: location.search }}
      className="font-medium text-slate-900 hover:text-brand-700 hover:underline focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none"
    >
      {kunde.name}
    </Link>
  )
}

const spalten = spalte.columns([
  spalte.accessor('kundennummer', {
    header: 'Nr.',
    cell: (info) => <span className="text-slate-500 tabular-nums">{info.getValue()}</span>,
  }),
  spalte.accessor('name', {
    header: 'Name',
    cell: (info) => <KundenLink kunde={info.row.original} />,
  }),
  spalte.accessor((kunde) => `${kunde.plz} ${kunde.ort}`, { id: 'ort', header: 'Ort' }),
  spalte.accessor('email', {
    header: 'E-Mail',
    cell: (info) => <span className="text-slate-600">{info.getValue() ?? '–'}</span>,
  }),
  spalte.accessor('zahlungsfristTage', {
    header: () => <span className="block text-right">Zahlungsfrist</span>,
    cell: (info) => <span className="block text-right tabular-nums">{info.getValue()} Tage</span>,
  }),
])

export function KundenSeite() {
  const [parameter, setParameter] = useSearchParams()
  const location = useLocation()
  const suche = parameter.get('suche') ?? ''
  const seite = Number(parameter.get('seite') ?? '0')
  const [eingabe, setEingabe] = useState(suche)
  const entprellt = useEntprellt(eingabe)
  const { hatRolle } = useRollen()

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

  const abfrage = useKundenSuchen(
    { suche: suche || undefined, seite, groesse: 25 },
    { query: { placeholderData: keepPreviousData } },
  )

  return (
    <>
      <Seitenkopf
        titel="Kunden"
        beschreibung="Debitoren mit Adresse, Zahlungsfrist und offenen Posten."
        aktionen={
          hatRolle('BUCHHALTUNG') && (
            <LinkButton to={{ pathname: 'neu', search: location.search }}>
              <Plus aria-hidden className="size-4" />
              Neuer Kunde
            </LinkButton>
          )
        }
      />
      <section aria-label="Kundenliste" className="rounded-lg border border-slate-200 bg-white">
        <div className="border-b border-slate-200 p-3">
          <label className="relative block max-w-sm">
            <span className="sr-only">Kunden suchen</span>
            <Search
              aria-hidden
              className="pointer-events-none absolute top-2.5 left-3 size-4 text-slate-400"
            />
            <Eingabe
              type="search"
              value={eingabe}
              placeholder="Name, Kundennummer oder Ort"
              className="pl-9"
              onChange={(ereignis) => {
                setEingabe(ereignis.target.value)
              }}
            />
          </label>
        </div>
        {abfrage.isPending ? (
          <Ladeanzeige className="px-3" />
        ) : abfrage.isError ? (
          <Meldung className="m-3">{fehlertext(abfrage.error)}</Meldung>
        ) : abfrage.data.inhalt.length === 0 ? (
          <LeererZustand symbol={Users} titel={suche ? 'Keine Treffer' : 'Noch keine Kunden'}>
            {suche ? `Kein Kunde passt zu «${suche}».` : 'Legen Sie den ersten Kunden an.'}
          </LeererZustand>
        ) : (
          <>
            <KundenTabelle kunden={abfrage.data.inhalt} />
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
      <Outlet />
    </>
  )
}

function KundenTabelle({ kunden }: { kunden: KundeDto[] }) {
  const location = useLocation()
  const navigate = useNavigate()
  const tabelle = useTable({
    features: funktionen,
    columns: spalten,
    data: kunden,
    getRowId: (kunde) => kunde.id,
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
          <Zeile
            key={zeile.id}
            className="cursor-pointer"
            onClick={() => {
              void navigate({ pathname: zeile.original.id, search: location.search })
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
  )
}
