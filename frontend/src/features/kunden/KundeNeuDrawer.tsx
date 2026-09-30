import { useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { useKundeAnlegen } from '@/shared/api/generiert/endpunkte'
import { invalidieren } from '@/shared/lib/http'
import { Drawer } from '@/shared/ui/Drawer'
import { alsEingabe, LEERER_KUNDE } from './kunde-formular'
import { KundeFormular } from './KundeFormular'

export function KundeNeuDrawer() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const anlegen = useKundeAnlegen()

  const schliessen = () => {
    void navigate({ pathname: '/kunden', search: location.search })
  }

  return (
    <Drawer titel="Neuer Kunde" onSchliessen={schliessen}>
      <KundeFormular
        startwerte={LEERER_KUNDE}
        kundennummerAenderbar
        speichernText="Kunde anlegen"
        onSpeichern={async (werte) => {
          const kunde = await anlegen.mutateAsync({ data: alsEingabe(werte) })
          await invalidieren(queryClient, '/api/kunden')
          void navigate(
            { pathname: `/kunden/${kunde.id}`, search: location.search },
            { replace: true },
          )
        }}
        onAbbrechen={schliessen}
      />
    </Drawer>
  )
}
