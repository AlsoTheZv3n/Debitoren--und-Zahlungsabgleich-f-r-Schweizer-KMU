import {
  getAuditLogLesenMockHandler,
  getAuditMock,
  getVerlaufDerRechnungMockHandler,
} from '@/shared/api/generiert/endpunkte/audit/audit.msw'
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
  getMahnstufenAuflistenMockHandler,
  getMahnstufenMock,
} from '@/shared/api/generiert/endpunkte/mahnstufen/mahnstufen.msw'
import {
  getMahnlaeufeAuflistenMockHandler,
  getMahnungenDerRechnungMockHandler,
  getMahnungenMock,
  getMahnungenSuchenMockHandler,
  getMahnungLesenMockHandler,
} from '@/shared/api/generiert/endpunkte/mahnungen/mahnungen.msw'
import {
  getMandantenAuflistenMockHandler,
  getMandantenMock,
  getMandantLesenMockHandler,
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
  getAltersstrukturLesenMockHandler,
  getReportingMock,
  getUebersichtLesenMockHandler,
  getZahlungseingaengeLesenMockHandler,
} from '@/shared/api/generiert/endpunkte/reporting/reporting.msw'
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
  DEMO_MAHNLAUF,
  DEMO_MAHNSTUFEN,
  DEMO_MAHNUNGEN,
  DEMO_MANDANT_DETAIL,
  demoMahnung,
} from './mahnwesen-daten'
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
import {
  DEMO_ALTERSSTRUKTUR,
  DEMO_UEBERSICHT,
  DEMO_VERLAUF,
  demoZahlungseingaenge,
} from './reporting-daten'
import { DEMO_DATEI, DEMO_KLAERUNG, demoVorschlag, demoZahlung } from './zahlung-daten'

const DEMO_RECHNUNGEN = [demoEntwurf(), demoRechnung()]

/**
 * Feste Antworten für Benutzer, Mandanten, Kunden, Rechnungen, Zahlungen, Mahnwesen und Dashboard, damit der
 * Mock-Modus stabil aussieht und Tests nicht von Zufallsdaten abhängen; alle übrigen Endpunkte beantworten die
 * von Orval generierten Handler mit Faker-Daten.
 */
export const handlers = [
  getAktuellerBenutzerMockHandler(demoBenutzer()),
  getMandantenAuflistenMockHandler(DEMO_MANDANTEN),
  getMandantLesenMockHandler(DEMO_MANDANT_DETAIL),
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
  getMahnungenSuchenMockHandler(({ request }) => {
    const parameter = new URL(request.url).searchParams
    const status = parameter.get('status')
    const suche = parameter.get('suche')?.toLowerCase() ?? ''
    return seite(
      DEMO_MAHNUNGEN.filter(
        (mahnung) =>
          (!status || mahnung.status === status) &&
          (mahnung.kundeName.toLowerCase().includes(suche) ||
            mahnung.rechnungsnummer.toLowerCase().includes(suche)),
      ),
    )
  }),
  getMahnungLesenMockHandler(
    ({ params }) => DEMO_MAHNUNGEN.find((mahnung) => mahnung.id === params.id) ?? demoMahnung(),
  ),
  getMahnungenDerRechnungMockHandler(({ params }) =>
    DEMO_MAHNUNGEN.filter(
      (mahnung) => mahnung.rechnungId === params.rechnungId && mahnung.status !== 'VORSCHLAG',
    ),
  ),
  getMahnlaeufeAuflistenMockHandler([DEMO_MAHNLAUF]),
  getMahnstufenAuflistenMockHandler(DEMO_MAHNSTUFEN),
  getUebersichtLesenMockHandler(DEMO_UEBERSICHT),
  getAltersstrukturLesenMockHandler(DEMO_ALTERSSTRUKTUR),
  getZahlungseingaengeLesenMockHandler(demoZahlungseingaenge()),
  getVerlaufDerRechnungMockHandler(DEMO_VERLAUF),
  getAuditLogLesenMockHandler(seite(DEMO_VERLAUF)),
  ...getBenutzerMock(),
  ...getMandantenMock(),
  ...getKundenMock(),
  ...getRechnungenMock(),
  ...getMwstMock(),
  ...getZahlungenMock(),
  ...getZahlungsdateienMock(),
  ...getZuordnungenMock(),
  ...getMahnungenMock(),
  ...getMahnstufenMock(),
  ...getReportingMock(),
  ...getAuditMock(),
]
