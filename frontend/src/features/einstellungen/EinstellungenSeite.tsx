import { NavLink, Outlet } from 'react-router'
import { cn } from '@/shared/lib/cn'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

const BEREICHE = [
  { ziel: 'mandant', titel: 'Mandant' },
  { ziel: 'mahnstufen', titel: 'Mahnstufen' },
  { ziel: 'mwst', titel: 'MWST-Sätze' },
  { ziel: 'audit', titel: 'Audit-Log' },
]

/** Nur für Admins; die Route ist durch die RollenSchranke geschützt. Die Bereiche sind Unterseiten. */
export function EinstellungenSeite() {
  return (
    <>
      <Seitenkopf
        titel="Einstellungen"
        beschreibung="Mandant, Mahnstufen und MWST-Sätze; dazu das Audit-Log aller Änderungen."
      />
      <nav aria-label="Bereiche" className="mb-4 flex flex-wrap gap-1 border-b border-slate-200">
        {BEREICHE.map((bereich) => (
          <NavLink
            key={bereich.ziel}
            to={bereich.ziel}
            className={({ isActive }) =>
              cn(
                '-mb-px border-b-2 px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none',
                isActive
                  ? 'border-brand-600 font-medium text-brand-700'
                  : 'border-transparent text-slate-600 hover:text-slate-900',
              )
            }
          >
            {bereich.titel}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </>
  )
}
