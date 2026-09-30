import type {
  VorschlagDto,
  ZahlungDto,
  ZahlungsdateiDto,
  ZuordnungDto,
} from '@/shared/api/generiert/modell'
import { demoRechnung } from './rechnung-daten'

/** Feste Zahlungen für Mock-Modus und Tests, passend zu den Rechnungen aus rechnung-daten.ts. */
export const DEMO_DATEI: ZahlungsdateiDto = {
  id: '019a3000-0000-7000-8000-000000000001',
  dateiname: 'camt054-2026-09-15.xml',
  nachrichtId: 'MSG-20260915',
  erstelltVonBankAm: '2026-09-15T04:30:00Z',
  importiertAm: '2026-09-15T06:02:00Z',
  importiertVon: 'buchhaltung',
  anzahlBuchungen: 14,
  anzahlNeu: 12,
  anzahlUebersprungen: 2,
  anzahlIgnoriert: 1,
  unbearbeitet: 0,
  zugeordnet: 9,
  inKlaerung: 3,
}

export function demoZahlung(ueberschreiben: Partial<ZahlungDto> = {}): ZahlungDto {
  return {
    id: '019a3100-0000-7000-8000-000000000001',
    version: 1,
    zahlungsdateiId: DEMO_DATEI.id,
    bankReferenz: 'ZKB-20260912-0001',
    buchungsdatum: '2026-09-12',
    valuta: '2026-09-12',
    betrag: 1840,
    rest: 1840,
    waehrung: 'CHF',
    auftraggeber: 'Keller Haustechnik',
    auftraggeberIban: 'CH5604835012345678009',
    mitteilung: 'Rechnung September',
    status: 'KLAERUNG',
    klaerungsgrund: 'OHNE_REFERENZ',
    ...ueberschreiben,
  }
}

/** Die Klärungsliste aus dem Wireframe der Projektplanung. */
export const DEMO_KLAERUNG: ZahlungDto[] = [
  demoZahlung(),
  demoZahlung({
    id: '019a3100-0000-7000-8000-000000000002',
    bankReferenz: 'ZKB-20260911-0002',
    valuta: '2026-09-11',
    betrag: 250,
    rest: 250,
    auftraggeber: 'M. Brunner',
    auftraggeberIban: undefined,
    referenz: '000000000000000020259999991',
    mitteilung: undefined,
    klaerungsgrund: 'REFERENZ_UNBEKANNT',
  }),
  demoZahlung({
    id: '019a3100-0000-7000-8000-000000000003',
    bankReferenz: 'ZKB-20260910-0003',
    valuta: '2026-09-10',
    betrag: 145.5,
    rest: 50,
    auftraggeber: 'Gasser Treuhand GmbH',
    referenz: '000000000000000020260004108',
    mitteilung: undefined,
    klaerungsgrund: 'UEBERZAHLUNG',
  }),
]

export function demoVorschlag(ueberschreiben: Partial<VorschlagDto> = {}): VorschlagDto {
  const rechnung = demoRechnung()
  return {
    rechnungId: rechnung.id,
    rechnungsnummer: rechnung.rechnungsnummer ?? '',
    kundeName: rechnung.empfaenger.name,
    faelligAm: rechnung.faelligAm,
    offen: 1840,
    betrag: 1840,
    waehrung: 'CHF',
    regel: 'BETRAG_UND_KUNDE',
    automatisch: false,
    begruendung: `Betrag und Auftraggeber passen zu Rechnung ${rechnung.rechnungsnummer ?? ''}`,
    ...ueberschreiben,
  }
}

export function demoZuordnung(ueberschreiben: Partial<ZuordnungDto> = {}): ZuordnungDto {
  const rechnung = demoRechnung()
  return {
    id: '019a3200-0000-7000-8000-000000000001',
    zahlungId: '019a3100-0000-7000-8000-000000000009',
    rechnungId: rechnung.id,
    rechnungsnummer: rechnung.rechnungsnummer,
    kundeName: rechnung.empfaenger.name,
    betrag: 900,
    waehrung: 'CHF',
    art: 'AUTO',
    regel: 'REFERENZ_EXAKT',
    erstelltVon: 'system',
    erstelltAm: '2026-09-15T06:02:05Z',
    valuta: '2026-09-14',
    auftraggeber: 'Keller Haustechnik AG',
    ...ueberschreiben,
  }
}
