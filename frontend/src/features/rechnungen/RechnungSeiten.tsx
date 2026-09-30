import { useLocation, useParams, useSearchParams } from 'react-router'
import { useKundeLesen, useRechnungLesen } from '@/shared/api/generiert/endpunkte'
import { heuteIso } from '@/shared/lib/format'
import { fehlertext } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { alsFormularwerte, neueRechnung } from './rechnung-formular'
import { RechnungAnsicht } from './RechnungAnsicht'
import { RechnungEditor } from './RechnungEditor'

/** `/rechnungen/neu`, auf Wunsch mit vorgewähltem Kunden: `?kunde=<id>`. */
export function NeueRechnungSeite() {
  const [parameter] = useSearchParams()
  const kundeId = parameter.get('kunde') ?? ''
  const kunde = useKundeLesen(kundeId, { query: { enabled: kundeId !== '' } })

  if (kundeId && kunde.isPending) {
    return <Ladeanzeige />
  }
  const wahl = kunde.data
    ? {
        id: kunde.data.id,
        name: kunde.data.name,
        kundennummer: kunde.data.kundennummer,
        zahlungsfristTage: kunde.data.zahlungsfristTage,
      }
    : null
  return <RechnungEditor startwerte={neueRechnung(heuteIso(), wahl)} />
}

/** `/rechnungen/:id`: Entwürfe im Editor, alles andere in der Ansicht. */
export function RechnungSeite() {
  const { id = '' } = useParams()
  const location = useLocation()
  const meldung = (location.state as { meldung?: string } | null)?.meldung
  const rechnung = useRechnungLesen(id)
  const { hatRolle } = useRollen()

  if (rechnung.isPending) {
    return <Ladeanzeige />
  }
  if (rechnung.isError) {
    return <Meldung>{fehlertext(rechnung.error)}</Meldung>
  }
  if (rechnung.data.status === 'ENTWURF' && hatRolle('BUCHHALTUNG')) {
    return (
      <RechnungEditor
        key={rechnung.data.id}
        rechnung={rechnung.data}
        startwerte={alsFormularwerte(rechnung.data)}
        meldung={meldung}
      />
    )
  }
  return <RechnungAnsicht rechnung={rechnung.data} meldung={meldung} />
}
