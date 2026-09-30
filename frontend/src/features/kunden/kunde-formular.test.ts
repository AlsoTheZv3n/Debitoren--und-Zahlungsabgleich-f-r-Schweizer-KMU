import { describe, expect, it } from 'vitest'
import { BRUNNER } from '@/mocks/rechnung-daten'
import {
  alsAenderung,
  alsEingabe,
  alsFormularwerte,
  kundeSchema,
  LEERER_KUNDE,
} from './kunde-formular'

describe('kundeSchema', () => {
  it('verlangt Name, PLZ und Ort und prüft Land und E-Mail', () => {
    const ergebnis = kundeSchema.safeParse({ ...LEERER_KUNDE, land: 'ch', email: 'keine-adresse' })

    expect(ergebnis.success).toBe(false)
    const fehler = Object.fromEntries(
      ergebnis.error?.issues.map((issue) => [issue.path.join('.'), issue.message]) ?? [],
    )
    expect(fehler).toEqual({
      name: 'Pflichtfeld',
      plz: 'Pflichtfeld',
      ort: 'Pflichtfeld',
      land: 'Ländercode mit zwei Grossbuchstaben, etwa CH',
      email: 'Keine gültige E-Mail-Adresse',
    })
  })

  it('meldet eine leere Zahlungsfrist als fehlende Angabe', () => {
    const ergebnis = kundeSchema.safeParse({
      ...LEERER_KUNDE,
      name: 'A',
      plz: '3000',
      ort: 'Bern',
      zahlungsfristTage: Number.NaN,
    })

    expect(ergebnis.error?.issues[0]?.message).toBe('Anzahl Tage angeben')
  })
})

describe('Umwandlung für die API', () => {
  it('schickt leere optionale Felder nicht mit', () => {
    const werte = kundeSchema.parse({
      ...LEERER_KUNDE,
      name: ' Muster AG ',
      plz: '3000',
      ort: 'Bern',
    })

    expect(alsEingabe(werte)).toEqual({
      kundennummer: undefined,
      name: 'Muster AG',
      strasse: undefined,
      hausnummer: undefined,
      plz: '3000',
      ort: 'Bern',
      land: 'CH',
      email: undefined,
      zahlungsfristTage: 30,
    })
  })

  it('ändert einen bestehenden Kunden mit seiner Version, ohne Kundennummer', () => {
    const werte = kundeSchema.parse({ ...alsFormularwerte(BRUNNER), ort: 'Biel' })
    const aenderung = alsAenderung(werte, BRUNNER.version)

    expect(aenderung).toMatchObject({ name: 'Marco Brunner', ort: 'Biel', version: 3 })
    expect(aenderung).not.toHaveProperty('kundennummer')
    expect(aenderung.email).toBeUndefined()
  })
})
