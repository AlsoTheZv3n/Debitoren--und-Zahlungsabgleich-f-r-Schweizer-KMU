import { describe, expect, it } from 'vitest'
import { demoEntwurf, KELLER } from '@/mocks/rechnung-daten'
import {
  alsEntwurf,
  alsFormularpfad,
  alsFormularwerte,
  alsVorschauanfrage,
  LEERE_POSITION,
  neueRechnung,
  rechnungSchema,
  type RechnungFormularWerte,
} from './rechnung-formular'

const kunde = { id: KELLER.id, name: KELLER.name, kundennummer: KELLER.kundennummer }

function werte(ueberschreiben: Partial<RechnungFormularWerte> = {}): RechnungFormularWerte {
  return {
    ...neueRechnung('2026-09-30', kunde),
    positionen: [
      {
        beschreibung: 'Service',
        menge: '6.5',
        einheit: 'Std.',
        einzelpreis: '145',
        mwstCode: 'NORMAL',
      },
    ],
    ...ueberschreiben,
  }
}

const fehlerVon = (eingabe: RechnungFormularWerte) => {
  const ergebnis = rechnungSchema.safeParse(eingabe)
  return ergebnis.success
    ? {}
    : Object.fromEntries(
        ergebnis.error.issues.map((issue) => [issue.path.join('.'), issue.message]),
      )
}

describe('rechnungSchema', () => {
  it('akzeptiert Schweizer Schreibweisen und wandelt sie für die API in Zahlen', () => {
    const ergebnis = rechnungSchema.parse(
      werte({
        zahlungsfristTage: '10',
        bemerkung: '  Danke für den Auftrag  ',
        positionen: [
          {
            ...LEERE_POSITION,
            beschreibung: 'Material',
            menge: '1’250,5',
            einzelpreis: "1'234.50",
          },
        ],
      }),
    )

    expect(alsEntwurf(ergebnis)).toEqual({
      kundeId: KELLER.id,
      rechnungsdatum: '2026-09-30',
      zahlungsfristTage: 10,
      waehrung: 'CHF',
      bemerkung: 'Danke für den Auftrag',
      positionen: [
        {
          beschreibung: 'Material',
          menge: 1250.5,
          einheit: undefined,
          einzelpreis: 1234.5,
          mwstCode: 'NORMAL',
        },
      ],
    })
  })

  it('lässt leere Frist und Bemerkung weg', () => {
    const entwurf = alsEntwurf(rechnungSchema.parse(werte()))
    expect(entwurf.zahlungsfristTage).toBeUndefined()
    expect(entwurf.bemerkung).toBeUndefined()
  })

  it('meldet fehlenden Kunden, fehlendes Datum und ungültige Positionen verständlich', () => {
    expect(
      fehlerVon(
        werte({
          kunde: null,
          rechnungsdatum: '',
          zahlungsfristTage: '400',
          positionen: [
            { ...LEERE_POSITION, beschreibung: '', menge: '0', einzelpreis: '12.345' },
            { ...LEERE_POSITION, beschreibung: 'X', menge: 'viel', einzelpreis: '' },
          ],
        }),
      ),
    ).toEqual({
      kunde: 'Kunde wählen',
      rechnungsdatum: 'Datum angeben',
      zahlungsfristTage: 'Ganze Tage von 0 bis 365',
      'positionen.0.beschreibung': 'Pflichtfeld',
      'positionen.0.menge': 'Grösser als null',
      'positionen.0.einzelpreis': 'Höchstens zwei Nachkommastellen',
      'positionen.1.menge': 'Keine gültige Zahl',
      'positionen.1.einzelpreis': 'Pflichtfeld',
    })
  })

  it('begrenzt Mengen auf drei Nachkommastellen und verlangt mindestens eine Position', () => {
    expect(
      fehlerVon(
        werte({
          positionen: [{ ...LEERE_POSITION, beschreibung: 'A', menge: '0.0001', einzelpreis: '1' }],
        }),
      ),
    ).toEqual({ 'positionen.0.menge': 'Höchstens drei Nachkommastellen' })
    expect(fehlerVon(werte({ positionen: [] }))).toEqual({ positionen: 'Mindestens eine Position' })
  })
})

describe('alsFormularwerte', () => {
  it('übernimmt einen gespeicherten Entwurf mit Preisen auf zwei Stellen', () => {
    const formular = alsFormularwerte(demoEntwurf())

    expect(formular.kunde).toEqual({ id: KELLER.id, name: KELLER.name })
    expect(formular.zahlungsfristTage).toBe('30')
    expect(formular.positionen[0]).toEqual({
      beschreibung: 'Service Heizungsanlage',
      menge: '6.5',
      einheit: 'Std.',
      einzelpreis: '145.00',
      mwstCode: 'NORMAL',
    })
  })
})

describe('alsVorschauanfrage', () => {
  it('lässt unfertige Zeilen weg und merkt sich, welche Zeilen berechnet werden', () => {
    const vorschau = alsVorschauanfrage(
      werte({
        kunde: null,
        zahlungsfristTage: 'x',
        positionen: [
          { ...LEERE_POSITION, einzelpreis: '' },
          { ...LEERE_POSITION, einzelpreis: '80' },
        ],
      }),
    )

    expect(vorschau).toEqual({
      anfrage: {
        kundeId: undefined,
        rechnungsdatum: '2026-09-30',
        zahlungsfristTage: undefined,
        waehrung: 'CHF',
        positionen: [
          {
            beschreibung: 'Vorschau',
            menge: 1,
            einheit: undefined,
            einzelpreis: 80,
            mwstCode: 'NORMAL',
          },
        ],
      },
      zeilen: [1],
    })
  })

  it('gibt ohne gültiges Rechnungsdatum keine Vorschau', () => {
    expect(alsVorschauanfrage(werte({ rechnungsdatum: '' }))).toBeNull()
  })
})

describe('alsFormularpfad', () => {
  it('übersetzt Feldnamen des Backends in Pfade des Formulars', () => {
    expect(alsFormularpfad('positionen[2].einzelpreis')).toBe('positionen.2.einzelpreis')
    expect(alsFormularpfad('entwurf.positionen[0].menge')).toBe('positionen.0.menge')
    expect(alsFormularpfad('kundeId')).toBe('kunde')
    expect(alsFormularpfad('rechnungsdatum')).toBe('rechnungsdatum')
  })
})
