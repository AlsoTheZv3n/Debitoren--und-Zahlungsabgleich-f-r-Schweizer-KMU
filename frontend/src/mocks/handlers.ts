import {
  getAktuellerBenutzerMockHandler,
  getBenutzerMock,
} from '@/shared/api/generiert/endpunkte/benutzer/benutzer.msw'
import {
  getKundeLesenMockHandler,
  getKundenMock,
  getKundenSuchenMockHandler,
} from '@/shared/api/generiert/endpunkte/kunden/kunden.msw'
import {
  getMandantenAuflistenMockHandler,
  getMandantenMock,
} from '@/shared/api/generiert/endpunkte/mandanten/mandanten.msw'
import {
  getMwstMock,
  getMwstSaetzeAuflistenMockHandler,
} from '@/shared/api/generiert/endpunkte/mwst/mwst.msw'
import {
  getRechnungenMock,
  getRechnungenSuchenMockHandler,
  getRechnungLesenMockHandler,
  getRechnungVorschauMockHandler,
  getRechnungZahlteilMockHandler,
} from '@/shared/api/generiert/endpunkte/rechnungen/rechnungen.msw'
import {
  getZahlungenMock,
  getZahlungenSuchenMockHandler,
  getZahlungLesenMockHandler,
  getZahlungVorschlaegeMockHandler,
  getZahlungZuordnungenMockHandler,
} from '@/shared/api/generiert/endpunkte/zahlungen/zahlungen.msw'
import {
  getZahlungsdateienAuflistenMockHandler,
  getZahlungsdateienMock,
} from '@/shared/api/generiert/endpunkte/zahlungsdateien/zahlungsdateien.msw'
import {
  getZuordnungenDerRechnungMockHandler,
  getZuordnungenMock,
} from '@/shared/api/generiert/endpunkte/zuordnungen/zuordnungen.msw'
import { DEMO_MANDANTEN, demoBenutzer } from './demo-daten'
import {
  alsZeile,
  DEMO_KUNDEN,
  DEMO_MWST_SAETZE,
  DEMO_ZAHLTEIL_SVG,
  KELLER,
  demoEntwurf,
  demoRechnung,
  demoVorschau,
  seite,
} from './rechnung-daten'
import { DEMO_DATEI, DEMO_KLAERUNG, demoVorschlag, demoZahlung } from './zahlung-daten'

const DEMO_RECHNUNGEN = [demoEntwurf(), demoRechnung()]

/**
 * Feste Antworten für Benutzer, Mandanten, Kunden, Rechnungen und Zahlungen, damit der Mock-Modus stabil aussieht
 * und Tests nicht von Zufallsdaten abhängen; alle übrigen Endpunkte beantworten die von Orval
 * generierten Handler mit Faker-Daten.
 */
export const handlers = [
  getAktuellerBenutzerMockHandler(demoBenutzer()),
  getMandantenAuflistenMockHandler(DEMO_MANDANTEN),
  getKundenSuchenMockHandler(({ request }) => {
    const suche = new URL(request.url).searchParams.get('suche')?.toLowerCase() ?? ''
    return seite(DEMO_KUNDEN.filter((kunde) => kunde.name.toLowerCase().includes(suche)))
  }),
  getKundeLesenMockHandler(
    ({ params }) => DEMO_KUNDEN.find((kunde) => kunde.id === params.id) ?? KELLER,
  ),
  getRechnungenSuchenMockHandler(({ request }) => {
    const parameter = new URL(request.url).searchParams
    const kundeId = parameter.get('kundeId')
    const status = parameter.get('status')
    const nurOffene = parameter.get('offen') === 'true'
    return seite(
      DEMO_RECHNUNGEN.filter(
        (rechnung) =>
          (!kundeId || rechnung.kundeId === kundeId) &&
          (!status || rechnung.status === status) &&
          (!nurOffene || rechnung.status === 'OFFEN' || rechnung.status === 'TEILBEZAHLT'),
      ).map(alsZeile),
    )
  }),
  getRechnungLesenMockHandler(
    ({ params }) => DEMO_RECHNUNGEN.find((rechnung) => rechnung.id === params.id) ?? demoRechnung(),
  ),
  getRechnungVorschauMockHandler(demoVorschau()),
  getRechnungZahlteilMockHandler(new Blob([DEMO_ZAHLTEIL_SVG], { type: 'image/svg+xml' })),
  getMwstSaetzeAuflistenMockHandler(DEMO_MWST_SAETZE),
  getZahlungsdateienAuflistenMockHandler(seite([DEMO_DATEI])),
  getZahlungenSuchenMockHandler(({ request }) => {
    const status = new URL(request.url).searchParams.get('status')
    return seite(DEMO_KLAERUNG.filter((zahlung) => !status || zahlung.status === status))
  }),
  getZahlungLesenMockHandler(
    ({ params }) => DEMO_KLAERUNG.find((zahlung) => zahlung.id === params.id) ?? demoZahlung(),
  ),
  getZahlungVorschlaegeMockHandler(({ params }) =>
    params.id === DEMO_KLAERUNG[0]?.id ? [demoVorschlag()] : [],
  ),
  getZahlungZuordnungenMockHandler([]),
  getZuordnungenDerRechnungMockHandler([]),
  ...getBenutzerMock(),
  ...getMandantenMock(),
  ...getKundenMock(),
  ...getRechnungenMock(),
  ...getMwstMock(),
  ...getZahlungenMock(),
  ...getZahlungsdateienMock(),
  ...getZuordnungenMock(),
]
