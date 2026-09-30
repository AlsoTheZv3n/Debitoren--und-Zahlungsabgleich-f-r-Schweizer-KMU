import type {
  AltersstrukturDto,
  AuditEintragDto,
  MonatseingangDto,
  UebersichtDto,
} from '@/shared/api/generiert/modell'
import { DEMO_DATEI } from './zahlung-daten'
import { demoRechnung } from './rechnung-daten'

/** Feste Kennzahlen und Audit-Einträge für Mock-Modus und Tests. */
export const DEMO_UEBERSICHT: UebersichtDto = {
  stichtag: '2026-09-30',
  offen: [
    { waehrung: 'CHF', betrag: 184250.4, anzahl: 212 },
    { waehrung: 'EUR', betrag: 6120, anzahl: 9 },
  ],
  ueberfaellig: [{ waehrung: 'CHF', betrag: 41380.15, anzahl: 57 }],
  inKlaerung: [{ waehrung: 'CHF', betrag: 2280.5, anzahl: 4 }],
  mahnvorschlaege: 3,
  entwuerfe: 6,
  zahlungen30Tage: 120,
  automatisch30Tage: 111,
  letzteImporte: [
    {
      id: DEMO_DATEI.id,
      dateiname: DEMO_DATEI.dateiname,
      importiertAm: DEMO_DATEI.importiertAm,
      importiertVon: DEMO_DATEI.importiertVon,
      neu: DEMO_DATEI.anzahlNeu,
      inKlaerung: DEMO_DATEI.inKlaerung,
    },
  ],
  letzterMahnlauf: {
    ausgefuehrtAm: '2026-09-30T04:00:00Z',
    ausgeloestVon: 'system',
    vorschlaege: 3,
  },
}

export const DEMO_ALTERSSTRUKTUR: AltersstrukturDto = {
  stichtag: '2026-09-30',
  waehrungen: [
    {
      waehrung: 'CHF',
      total: 184250.4,
      baender: [
        { band: 'NICHT_FAELLIG', betrag: 142870.25, anzahl: 155 },
        { band: 'TAGE_1_30', betrag: 23410.1, anzahl: 31 },
        { band: 'TAGE_31_60', betrag: 9820.05, anzahl: 14 },
        { band: 'TAGE_61_90', betrag: 4150, anzahl: 6 },
        { band: 'UEBER_90', betrag: 4000, anzahl: 6 },
      ],
    },
    {
      waehrung: 'EUR',
      total: 6120,
      baender: [
        { band: 'NICHT_FAELLIG', betrag: 6120, anzahl: 9 },
        { band: 'TAGE_1_30', betrag: 0, anzahl: 0 },
        { band: 'TAGE_31_60', betrag: 0, anzahl: 0 },
        { band: 'TAGE_61_90', betrag: 0, anzahl: 0 },
        { band: 'UEBER_90', betrag: 0, anzahl: 0 },
      ],
    },
  ],
}

/** Zwölf Monate bis September 2026; die Automatik wächst, wie es nach der Einführung zu erwarten ist. */
export function demoZahlungseingaenge(): MonatseingangDto[] {
  const betraege = [
    61200, 58400, 70150, 64800, 59900, 66300, 72400, 68100, 74900, 71250, 69800, 38420.5,
  ]
  return betraege.map((betrag, index) => {
    const monat = new Date(Date.UTC(2025, 9 + index, 1))
    return {
      monat: `${String(monat.getUTCFullYear())}-${String(monat.getUTCMonth() + 1).padStart(2, '0')}`,
      waehrung: 'CHF',
      betrag,
      automatisch: Math.round(betrag * (0.82 + index * 0.01) * 100) / 100,
      anzahl: Math.round(betrag / 520),
    }
  })
}

export const DEMO_VERLAUF: AuditEintragDto[] = [
  {
    revision: 812,
    zeitpunkt: '2026-09-15T06:02:10Z',
    benutzer: 'buchhaltung',
    objektart: 'ZUORDNUNG',
    objektId: '019a5000-0000-7000-8000-000000000001',
    aktion: 'ANGELEGT',
    bezeichnung: 'Zahlung zu RE-2026-0412',
    rechnungId: demoRechnung().id,
    aenderungen: [
      { feld: 'Betrag', nachher: 'CHF 500.00' },
      { feld: 'Art', nachher: 'von Hand' },
    ],
  },
  {
    revision: 640,
    zeitpunkt: '2026-09-01T08:15:00Z',
    benutzer: 'buchhaltung',
    objektart: 'RECHNUNG',
    objektId: demoRechnung().id,
    aktion: 'GEAENDERT',
    bezeichnung: 'RE-2026-0412 · Keller Haustechnik AG',
    rechnungId: demoRechnung().id,
    aenderungen: [
      { feld: 'Status', vorher: 'Entwurf', nachher: 'Offen' },
      { feld: 'Nummer', nachher: 'RE-2026-0412' },
    ],
  },
]
