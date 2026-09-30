import { Navigate, type RouteObject } from 'react-router'
import { DashboardSeite } from '@/features/dashboard/DashboardSeite'
import { EinstellungenSeite } from '@/features/einstellungen/EinstellungenSeite'
import { MahnwesenSeite } from '@/features/mahnwesen/MahnwesenSeite'
import { ImportSeite } from '@/features/zahlungen/ImportSeite'
import { KlaerungSeite } from '@/features/zahlungen/KlaerungSeite'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Anmeldeschranke } from './anmeldung/Anmeldeschranke'
import { AppLayout } from './layout/AppLayout'
import { NichtGefundenSeite } from './NichtGefundenSeite'
import { RollenSchranke } from './RollenSchranke'
import type { RoutenHandle } from './routen-handle'

const titel = (text: string): RoutenHandle => ({ titel: text })

// Kunden und Rechnungen bringen Formulare, Validierung und Tabellen mit; sie laden erst beim ersten
// Aufruf. Bis dahin bleibt die bisherige Seite stehen.
const kunden = () => import('@/features/kunden/KundenSeite')
const kundeDrawer = () => import('@/features/kunden/KundeDrawer')
const kundeNeu = () => import('@/features/kunden/KundeNeuDrawer')
const rechnungsliste = () => import('@/features/rechnungen/RechnungenSeite')
const rechnungSeiten = () => import('@/features/rechnungen/RechnungSeiten')

export const routen: RouteObject[] = [
  {
    element: <Anmeldeschranke />,
    hydrateFallbackElement: <Ladeanzeige className="justify-center" />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <DashboardSeite />, handle: titel('Dashboard') },
          {
            path: 'rechnungen',
            handle: titel('Rechnungen'),
            children: [
              {
                index: true,
                lazy: async () => ({ Component: (await rechnungsliste()).RechnungenSeite }),
              },
              {
                path: 'neu',
                lazy: async () => {
                  const { NeueRechnungSeite } = await rechnungSeiten()
                  return {
                    element: (
                      <RollenSchranke rolle="BUCHHALTUNG">
                        <NeueRechnungSeite />
                      </RollenSchranke>
                    ),
                  }
                },
                handle: titel('Neue Rechnung'),
              },
              {
                path: ':id',
                lazy: async () => ({ Component: (await rechnungSeiten()).RechnungSeite }),
                handle: titel('Rechnung'),
              },
            ],
          },
          {
            path: 'kunden',
            lazy: async () => ({ Component: (await kunden()).KundenSeite }),
            handle: titel('Kunden'),
            children: [
              {
                path: 'neu',
                lazy: async () => {
                  const { KundeNeuDrawer } = await kundeNeu()
                  return {
                    element: (
                      <RollenSchranke rolle="BUCHHALTUNG">
                        <KundeNeuDrawer />
                      </RollenSchranke>
                    ),
                  }
                },
              },
              {
                path: ':id',
                lazy: async () => ({ Component: (await kundeDrawer()).KundeDrawer }),
              },
            ],
          },
          {
            path: 'zahlungen',
            handle: titel('Zahlungen'),
            children: [
              { index: true, element: <Navigate to="import" replace /> },
              { path: 'import', element: <ImportSeite />, handle: titel('Import') },
              { path: 'klaerung', element: <KlaerungSeite />, handle: titel('Klärungsliste') },
            ],
          },
          { path: 'mahnwesen', element: <MahnwesenSeite />, handle: titel('Mahnwesen') },
          {
            path: 'einstellungen',
            element: (
              <RollenSchranke rolle="ADMIN">
                <EinstellungenSeite />
              </RollenSchranke>
            ),
            handle: titel('Einstellungen'),
          },
          { path: '*', element: <NichtGefundenSeite />, handle: titel('Nicht gefunden') },
        ],
      },
    ],
  },
]
