import { useQueryClient } from '@tanstack/react-query'
import { FilePlus2, Pencil } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import {
  useKundeAendern,
  useKundeLesen,
  useRechnungenSuchen,
} from '@/shared/api/generiert/endpunkte'
import type { KundeDto } from '@/shared/api/generiert/modell'
import { formatDatum } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { Betrag } from '@/shared/ui/Betrag'
import { Button, LinkButton } from '@/shared/ui/Button'
import { Drawer } from '@/shared/ui/Drawer'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { RechnungsstatusBadge } from '@/shared/ui/RechnungsstatusBadge'
import { alsAenderung, alsFormularwerte } from './kunde-formular'
import { KundeFormular } from './KundeFormular'

/** Details eines Kunden im seitlichen Fenster; die Liste bleibt dahinter sichtbar. */
export function KundeDrawer() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { hatRolle } = useRollen()
  const [bearbeiten, setBearbeiten] = useState(false)
  const kunde = useKundeLesen(id)
  const aendern = useKundeAendern()

  const schliessen = () => {
    void navigate({ pathname: '/kunden', search: location.search })
  }

  return (
    <Drawer
      titel={kunde.data?.name ?? 'Kunde'}
      untertitel={kunde.data?.kundennummer}
      onSchliessen={schliessen}
    >
      {kunde.isPending ? (
        <Ladeanzeige />
      ) : kunde.isError ? (
        <Meldung>{fehlertext(kunde.error)}</Meldung>
      ) : bearbeiten ? (
        <KundeFormular
          startwerte={alsFormularwerte(kunde.data)}
          kundennummerAenderbar={false}
          speichernText="Änderungen speichern"
          onSpeichern={async (werte) => {
            await aendern.mutateAsync({ id, data: alsAenderung(werte, kunde.data.version) })
            await invalidieren(queryClient, '/api/kunden')
            setBearbeiten(false)
          }}
          onAbbrechen={() => {
            setBearbeiten(false)
          }}
        />
      ) : (
        <div className="space-y-6">
          <Stammdaten kunde={kunde.data} />
          {hatRolle('BUCHHALTUNG') && (
            <div className="flex flex-wrap gap-2">
              <Button
                variante="sekundaer"
                onClick={() => {
                  setBearbeiten(true)
                }}
              >
                <Pencil aria-hidden className="size-4" />
                Bearbeiten
              </Button>
              <LinkButton variante="sekundaer" to={`/rechnungen/neu?kunde=${id}`}>
                <FilePlus2 aria-hidden className="size-4" />
                Neue Rechnung
              </LinkButton>
            </div>
          )}
          <OffenePosten kundeId={id} />
        </div>
      )}
    </Drawer>
  )
}

function Stammdaten({ kunde }: { kunde: KundeDto }) {
  const strasse = [kunde.strasse, kunde.hausnummer].filter(Boolean).join(' ')
  return (
    <dl className="grid grid-cols-3 gap-x-4 gap-y-3 text-sm">
      <dt className="text-slate-500">Adresse</dt>
      <dd className="col-span-2">
        {strasse && <span className="block">{strasse}</span>}
        <span className="block">
          {kunde.land === 'CH' ? '' : `${kunde.land}-`}
          {kunde.plz} {kunde.ort}
        </span>
      </dd>
      <dt className="text-slate-500">E-Mail</dt>
      <dd className="col-span-2">{kunde.email ?? '–'}</dd>
      <dt className="text-slate-500">Zahlungsfrist</dt>
      <dd className="col-span-2">{kunde.zahlungsfristTage} Tage</dd>
    </dl>
  )
}

function OffenePosten({ kundeId }: { kundeId: string }) {
  const posten = useRechnungenSuchen({
    kundeId,
    offen: true,
    groesse: 50,
    sortierung: 'FAELLIGKEIT_AUFSTEIGEND',
  })

  return (
    <section aria-labelledby="offene-posten">
      <h3 id="offene-posten" className="mb-2 text-sm font-semibold text-slate-900">
        Offene Posten
      </h3>
      {posten.isPending ? (
        <Ladeanzeige />
      ) : posten.isError ? (
        <Meldung>{fehlertext(posten.error)}</Meldung>
      ) : posten.data.inhalt.length === 0 ? (
        <p className="text-sm text-slate-500">Keine offenen Rechnungen.</p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {posten.data.inhalt.map((rechnung) => (
            <li key={rechnung.id} className="flex items-center gap-3 px-3 py-2 text-sm">
              <Link
                to={`/rechnungen/${rechnung.id}`}
                className="font-medium text-brand-700 hover:underline"
              >
                {rechnung.rechnungsnummer}
              </Link>
              <span className="text-slate-500">fällig {formatDatum(rechnung.faelligAm)}</span>
              <RechnungsstatusBadge status={rechnung.status} ueberfaellig={rechnung.ueberfaellig} />
              <Betrag className="ml-auto" betrag={rechnung.offen} waehrung={rechnung.waehrung} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
