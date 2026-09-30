import type {
  KundeDto,
  MwstSatzDto,
  RechnungDto,
  RechnungZeileDto,
  VorschauDto,
} from '@/shared/api/generiert/modell'

/**
 * Feste Kunden und Rechnungen für den Mock-Modus und die Tests. Beträge und Referenzen sind so
 * gewählt, wie das Backend sie liefern würde; gerechnet wird hier nichts.
 */
export const KELLER: KundeDto = {
  id: '019a1000-0000-7000-8000-000000000001',
  version: 0,
  kundennummer: 'K-10001',
  name: 'Keller Haustechnik AG',
  strasse: 'Industriestrasse',
  hausnummer: '12',
  plz: '3008',
  ort: 'Bern',
  land: 'CH',
  email: 'buchhaltung@keller-haustechnik.example',
  zahlungsfristTage: 30,
}

export const BRUNNER: KundeDto = {
  id: '019a1000-0000-7000-8000-000000000002',
  version: 3,
  kundennummer: 'K-10002',
  name: 'Marco Brunner',
  strasse: 'Seevorstadt',
  hausnummer: '5',
  plz: '2502',
  ort: 'Biel/Bienne',
  land: 'CH',
  zahlungsfristTage: 10,
}

export const GASSER: KundeDto = {
  id: '019a1000-0000-7000-8000-000000000003',
  version: 1,
  kundennummer: 'K-10003',
  name: 'Gasser Treuhand GmbH',
  plz: '6003',
  ort: 'Luzern',
  land: 'CH',
  email: 'info@gasser-treuhand.example',
  zahlungsfristTage: 30,
}

export const DEMO_KUNDEN = [KELLER, BRUNNER, GASSER]

export const DEMO_MWST_SAETZE: MwstSatzDto[] = [
  satz('NORMAL', 'Normalsatz', 8.1),
  satz('REDUZIERT', 'Reduzierter Satz', 2.6),
  satz('BEHERBERGUNG', 'Beherbergung', 3.8),
  satz('OHNE', 'Ohne MWST', 0),
]

function satz(code: MwstSatzDto['code'], bezeichnung: string, prozent: number): MwstSatzDto {
  return { id: `mwst-${code}`, code, bezeichnung, satz: prozent, gueltigAb: '2024-01-01' }
}

/** Platzhalter für den QR-Zahlteil; das echte SVG erzeugt das Backend. */
export const DEMO_ZAHLTEIL_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" width="210mm" height="105mm" viewBox="0 0 210 105">' +
  '<rect width="210" height="105" fill="#fff" stroke="#000" stroke-dasharray="2"/>' +
  '<text x="5" y="10" font-family="sans-serif" font-size="5">Empfangsschein</text>' +
  '<text x="67" y="10" font-family="sans-serif" font-size="5">Zahlteil</text></svg>'

export const DEMO_SUMMEN: RechnungDto['summen'] = {
  netto: 1702.13,
  mwst: [{ code: 'NORMAL', prozent: 8.1, basis: 1702.13, betrag: 137.87 }],
  mwstTotal: 137.87,
  rundung: 0,
  total: 1840,
}

export function demoRechnung(ueberschreiben: Partial<RechnungDto> = {}): RechnungDto {
  return {
    id: '019a2000-0000-7000-8000-000000000412',
    version: 1,
    kundeId: KELLER.id,
    rechnungsnummer: 'RE-2026-0412',
    status: 'OFFEN',
    rechnungsdatum: '2026-09-01',
    faelligAm: '2026-10-01',
    zahlungsfristTage: 30,
    waehrung: 'CHF',
    referenz: '00 00000 00000 00000 00000 04126',
    referenzart: 'QR',
    freigegebenAm: '2026-09-01T08:15:00Z',
    ueberfaellig: false,
    empfaenger: {
      name: KELLER.name,
      strasse: KELLER.strasse,
      hausnummer: KELLER.hausnummer,
      plz: KELLER.plz,
      ort: KELLER.ort,
      land: KELLER.land,
    },
    positionen: [
      {
        position: 1,
        beschreibung: 'Service Heizungsanlage',
        menge: 6.5,
        einheit: 'Std.',
        einzelpreis: 145,
        mwstCode: 'NORMAL',
        mwstProzent: 8.1,
        betrag: 942.5,
      },
      {
        position: 2,
        beschreibung: 'Umwälzpumpe Grundfos',
        menge: 1,
        einheit: 'Stk.',
        einzelpreis: 759.63,
        mwstCode: 'NORMAL',
        mwstProzent: 8.1,
        betrag: 759.63,
      },
    ],
    summen: DEMO_SUMMEN,
    mahngebuehren: 0,
    offen: 1840,
    ...ueberschreiben,
  }
}

export function demoEntwurf(ueberschreiben: Partial<RechnungDto> = {}): RechnungDto {
  return demoRechnung({
    id: '019a2000-0000-7000-8000-000000000999',
    version: 0,
    status: 'ENTWURF',
    rechnungsnummer: undefined,
    referenz: undefined,
    referenzart: undefined,
    freigegebenAm: undefined,
    offen: 0,
    rechnungsdatum: '2026-09-28',
    faelligAm: '2026-10-28',
    ...ueberschreiben,
  })
}

export function alsZeile(rechnung: RechnungDto): RechnungZeileDto {
  return {
    id: rechnung.id,
    version: rechnung.version,
    rechnungsnummer: rechnung.rechnungsnummer,
    status: rechnung.status,
    kundeId: rechnung.kundeId,
    kundeName: rechnung.empfaenger.name,
    rechnungsdatum: rechnung.rechnungsdatum,
    faelligAm: rechnung.faelligAm,
    waehrung: rechnung.waehrung,
    total: rechnung.summen.total,
    offen: rechnung.offen,
    ueberfaellig: rechnung.ueberfaellig,
  }
}

export function demoVorschau(ueberschreiben: Partial<VorschauDto> = {}): VorschauDto {
  return {
    summen: DEMO_SUMMEN,
    positionsbetraege: [942.5, 759.63],
    zahlteilSvg: DEMO_ZAHLTEIL_SVG,
    ...ueberschreiben,
  }
}

/** Eine Seite im Format der API. */
export function seite<T>(inhalt: T[], nummer = 0, groesse = 25) {
  return {
    inhalt,
    seite: nummer,
    groesse,
    anzahlElemente: inhalt.length,
    anzahlSeiten: Math.max(1, Math.ceil(inhalt.length / groesse)),
  }
}
