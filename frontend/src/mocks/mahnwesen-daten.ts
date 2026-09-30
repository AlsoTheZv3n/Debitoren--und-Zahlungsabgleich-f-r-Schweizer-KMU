import type {
  MahnlaufDto,
  MahnstufeDto,
  MahnungDto,
  MandantDetailDto,
} from '@/shared/api/generiert/modell'
import { BERNER_WERKSTATT } from './demo-daten'
import { BRUNNER, GASSER, KELLER, demoRechnung } from './rechnung-daten'

/** Feste Mahnungen, Mahnstufen und Mandantenangaben für Mock-Modus und Tests. */
export function demoMahnung(ueberschreiben: Partial<MahnungDto> = {}): MahnungDto {
  const rechnung = demoRechnung()
  return {
    id: '019a4000-0000-7000-8000-000000000001',
    version: 0,
    rechnungId: rechnung.id,
    rechnungsnummer: rechnung.rechnungsnummer ?? 'RE-2026-0412',
    kundeId: KELLER.id,
    kundeName: KELLER.name,
    faelligAm: '2026-08-20',
    tageUeberfaellig: 41,
    stufe: 2,
    bezeichnung: '1. Mahnung',
    mahndatum: '2026-09-30',
    fristBis: '2026-10-10',
    gebuehr: 0,
    waehrung: 'CHF',
    status: 'VORSCHLAG',
    zuZahlen: 1840,
    aktuellOffen: 1840,
    ...ueberschreiben,
  }
}

export const DEMO_VORSCHLAEGE: MahnungDto[] = [
  demoMahnung(),
  demoMahnung({
    id: '019a4000-0000-7000-8000-000000000002',
    rechnungId: '019a2000-0000-7000-8000-000000000388',
    rechnungsnummer: 'RE-2026-0388',
    kundeId: GASSER.id,
    kundeName: GASSER.name,
    faelligAm: '2026-08-05',
    tageUeberfaellig: 56,
    stufe: 3,
    bezeichnung: '2. Mahnung',
    gebuehr: 20,
    zuZahlen: 115.5,
    aktuellOffen: 95.5,
  }),
  demoMahnung({
    id: '019a4000-0000-7000-8000-000000000003',
    rechnungId: '019a2000-0000-7000-8000-000000000401',
    rechnungsnummer: 'RE-2026-0401',
    kundeId: BRUNNER.id,
    kundeName: BRUNNER.name,
    faelligAm: '2026-09-18',
    tageUeberfaellig: 12,
    stufe: 1,
    bezeichnung: 'Zahlungserinnerung',
    zuZahlen: 250,
    aktuellOffen: 250,
  }),
]

export const DEMO_MAHNUNGEN: MahnungDto[] = [
  ...DEMO_VORSCHLAEGE,
  demoMahnung({
    id: '019a4000-0000-7000-8000-000000000010',
    version: 2,
    stufe: 1,
    bezeichnung: 'Zahlungserinnerung',
    mahndatum: '2026-09-10',
    fristBis: '2026-09-20',
    status: 'VERSENDET',
    versandweg: 'EMAIL',
    empfaengerEmail: KELLER.email,
    freigegebenAm: '2026-09-10T06:05:00Z',
    freigegebenVon: 'buchhaltung',
    versendetAm: '2026-09-10T06:05:04Z',
  }),
  demoMahnung({
    id: '019a4000-0000-7000-8000-000000000011',
    version: 1,
    rechnungId: '019a2000-0000-7000-8000-000000000377',
    rechnungsnummer: 'RE-2026-0377',
    kundeId: BRUNNER.id,
    kundeName: BRUNNER.name,
    stufe: 1,
    bezeichnung: 'Zahlungserinnerung',
    mahndatum: '2026-09-25',
    fristBis: '2026-10-05',
    status: 'FREIGEGEBEN',
    versandweg: 'POST',
    freigegebenAm: '2026-09-25T06:05:00Z',
    freigegebenVon: 'buchhaltung',
    zuZahlen: 480,
  }),
]

export const DEMO_MAHNLAUF: MahnlaufDto = {
  id: '019a4100-0000-7000-8000-000000000001',
  stichtag: '2026-09-30',
  ausgefuehrtAm: '2026-09-30T04:00:00Z',
  ausgeloestVon: 'system',
  geprueft: 14,
  vorschlaege: 3,
  verworfen: 0,
}

export const DEMO_MAHNSTUFEN: MahnstufeDto[] = [
  {
    id: '019a4200-0000-7000-8000-000000000001',
    version: 0,
    stufe: 1,
    bezeichnung: 'Zahlungserinnerung',
    tageNachFaelligkeit: 10,
    mindestabstandTage: 0,
    fristTage: 10,
    gebuehr: 0,
    textvorlage:
      'Sicher ist es Ihnen im Alltag entgangen: Unsere Rechnung {rechnungsnummer} war am {faelligAm} fällig und ist noch offen.',
  },
  {
    id: '019a4200-0000-7000-8000-000000000002',
    version: 0,
    stufe: 2,
    bezeichnung: '1. Mahnung',
    tageNachFaelligkeit: 30,
    mindestabstandTage: 14,
    fristTage: 10,
    gebuehr: 0,
    textvorlage:
      'Trotz unserer Zahlungserinnerung ist die Rechnung {rechnungsnummer} noch nicht bezahlt.',
  },
  {
    id: '019a4200-0000-7000-8000-000000000003',
    version: 0,
    stufe: 3,
    bezeichnung: '2. Mahnung',
    tageNachFaelligkeit: 45,
    mindestabstandTage: 14,
    fristTage: 10,
    gebuehr: 20,
    textvorlage:
      'Leider ist die Rechnung {rechnungsnummer} auch nach unserer Mahnung noch offen. Wir verrechnen eine Mahngebühr von {gebuehr}.',
  },
]

export const DEMO_MANDANT_DETAIL: MandantDetailDto = {
  id: BERNER_WERKSTATT.id,
  version: 3,
  name: BERNER_WERKSTATT.name,
  strasse: 'Marktgasse',
  hausnummer: '28',
  plz: '3011',
  ort: 'Bern',
  land: 'CH',
  iban: 'CH5800791123000889012',
  qrIban: 'CH4431999123000889012',
  mwstNummer: 'CHE-100.200.300 MWST',
  email: 'rechnungen@berner-werkstatt.example',
  rundungFuenfRappen: true,
  referenzart: 'QR',
}
