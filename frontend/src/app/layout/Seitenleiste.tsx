import {
  BellRing,
  FileText,
  Landmark,
  LayoutDashboard,
  Settings,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router'
import { useZahlungenSuchen } from '@/shared/api/generiert/endpunkte'
import { cn } from '@/shared/lib/cn'
import { useRollen, type Rolle } from '@/shared/lib/rollen'

interface Eintrag {
  ziel: string
  titel: string
  symbol: LucideIcon
  ende?: boolean
  rolle?: Rolle
  unterpunkte?: { ziel: string; titel: string; zaehler?: boolean }[]
}

const EINTRAEGE: Eintrag[] = [
  { ziel: '/', titel: 'Dashboard', symbol: LayoutDashboard, ende: true },
  { ziel: '/rechnungen', titel: 'Rechnungen', symbol: FileText },
  { ziel: '/kunden', titel: 'Kunden', symbol: Users },
  {
    ziel: '/zahlungen',
    titel: 'Zahlungen',
    symbol: Landmark,
    unterpunkte: [
      { ziel: '/zahlungen/import', titel: 'Import' },
      { ziel: '/zahlungen/klaerung', titel: 'Klärungsliste', zaehler: true },
    ],
  },
  { ziel: '/mahnwesen', titel: 'Mahnwesen', symbol: BellRing },
  { ziel: '/einstellungen', titel: 'Einstellungen', symbol: Settings, rolle: 'ADMIN' },
]

const verweis =
  'flex items-center gap-2.5 rounded-lg px-2.5 py-1.5 text-sm whitespace-nowrap text-slate-700 transition-colors hover:bg-slate-100 hover:text-slate-900 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none'
const aktiverVerweis =
  'bg-brand-50 font-medium text-brand-700 hover:bg-brand-50 hover:text-brand-700'

export function Seitenleiste() {
  const { hatRolle } = useRollen()
  const { pathname } = useLocation()
  const sichtbar = EINTRAEGE.filter((eintrag) => !eintrag.rolle || hatRolle(eintrag.rolle))
  // Die Klärungsliste ist der wichtigste Arbeitsbildschirm; ihr Zähler hält sich von selbst aktuell.
  const klaerung = useZahlungenSuchen(
    { status: 'KLAERUNG', groesse: 1 },
    { query: { refetchInterval: 60_000 } },
  )
  const offeneKlaerungen = klaerung.data?.anzahlElemente ?? 0

  return (
    <nav
      aria-label="Hauptnavigation"
      className="min-w-0 border-b border-slate-200 bg-white md:border-r md:border-b-0"
    >
      <ul className="flex gap-1 overflow-x-auto p-2 md:sticky md:top-14 md:flex-col md:p-3">
        {sichtbar.map(({ ziel, titel, symbol: Zeichen, ende, unterpunkte }) => (
          <li key={ziel}>
            <NavLink
              to={unterpunkte?.[0]?.ziel ?? ziel}
              end={ende}
              className={({ isActive }) =>
                cn(
                  verweis,
                  (isActive || (unterpunkte && pathname.startsWith(ziel + '/'))) && aktiverVerweis,
                )
              }
            >
              <Zeichen aria-hidden className="size-4 shrink-0" />
              {titel}
            </NavLink>
            {unterpunkte && (
              <ul className="mt-1 ml-6 hidden space-y-0.5 border-l border-slate-200 pl-2 md:block">
                {unterpunkte.map((punkt) => (
                  <li key={punkt.ziel}>
                    <NavLink
                      to={punkt.ziel}
                      className={({ isActive }) =>
                        cn(verweis, 'py-1', isActive && 'font-medium text-brand-700')
                      }
                    >
                      {punkt.titel}
                      {punkt.zaehler && offeneKlaerungen > 0 && ' '}
                      {punkt.zaehler && offeneKlaerungen > 0 && (
                        <Zaehler anzahl={offeneKlaerungen} />
                      )}
                    </NavLink>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </nav>
  )
}

/** Anzahl ungeklärter Zahlungen; für Screenreader mit Text statt nur einer Zahl. */
function Zaehler({ anzahl }: { anzahl: number }) {
  return (
    <span className="ml-auto rounded-full bg-violet-100 px-1.5 text-xs font-medium text-violet-700 tabular-nums">
      {anzahl}
      <span className="sr-only"> ungeklärt</span>
    </span>
  )
}
