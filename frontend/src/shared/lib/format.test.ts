import { describe, expect, it } from 'vitest'
import {
  formatBetrag,
  formatDatum,
  formatMenge,
  formatProzent,
  formatReferenz,
  formatZeitpunkt,
  heuteIso,
  initialen,
} from './format'

/** Geschütztes Leerzeichen zwischen Währung und Betrag. */
const NBSP = String.fromCodePoint(0xa0)

describe('formatBetrag', () => {
  it('nutzt das Schweizer Tausendertrennzeichen und zwei Nachkommastellen', () => {
    expect(formatBetrag(1840, 'CHF')).toBe(`CHF${NBSP}1’840.00`)
    expect(formatBetrag(1234567.5, 'CHF')).toBe(`CHF${NBSP}1’234’567.50`)
    expect(formatBetrag(95.5, 'CHF')).toBe(`CHF${NBSP}95.50`)
  })

  it('zeigt die Währung des Betrags', () => {
    expect(formatBetrag(420, 'EUR')).toBe(`EUR${NBSP}420.00`)
  })

  it('rundet auf Rappen', () => {
    expect(formatBetrag(0.125, 'CHF')).toBe(`CHF${NBSP}0.13`)
  })

  it('zeigt negative Beträge mit Minuszeichen', () => {
    expect(formatBetrag(-250, 'CHF')).toMatch(new RegExp(`^CHF${NBSP}?-250\\.00$`))
  })

  it('bricht nie zwischen Währung und Betrag um', () => {
    expect(formatBetrag(1840, 'CHF')).not.toMatch(/ /)
  })
})

describe('formatDatum', () => {
  it('formatiert ein reines Datum ohne Zeitzonenverschiebung', () => {
    expect(formatDatum('2026-09-12')).toBe('12.09.2026')
    expect(formatDatum('2026-01-01')).toBe('01.01.2026')
  })

  it('zeigt Zeitpunkte in Schweizer Zeit', () => {
    expect(formatDatum('2026-09-29T23:30:00Z')).toBe('30.09.2026')
  })

  it('verweigert ungültige Werte', () => {
    expect(() => formatDatum('gestern')).toThrow(RangeError)
  })
})

describe('initialen', () => {
  it('nimmt den ersten und den letzten Namen', () => {
    expect(initialen('Andrea Meier')).toBe('AM')
    expect(initialen('Lea Maria Steiner')).toBe('LS')
    expect(initialen('admin')).toBe('A')
    expect(initialen('  ')).toBe('')
  })
})

describe('formatMenge und formatProzent', () => {
  it('schreiben Mengen und Sätze wie das Rechnungs-PDF', () => {
    expect(formatMenge(1250.5)).toBe('1’250.5')
    expect(formatMenge(0.125)).toBe('0.125')
    expect(formatProzent(8.1)).toBe(`8.1${NBSP}%`)
    expect(formatProzent(0)).toBe(`0.0${NBSP}%`)
  })
})

describe('heuteIso', () => {
  it('nimmt das Datum in Schweizer Zeit, auch kurz nach Mitternacht', () => {
    expect(heuteIso(new Date('2026-09-30T22:30:00Z'))).toBe('2026-10-01')
    expect(heuteIso(new Date('2026-12-31T12:00:00Z'))).toBe('2026-12-31')
  })
})

describe('formatReferenz und formatZeitpunkt', () => {
  it('gruppiert Referenzen wie auf dem Zahlteil', () => {
    expect(formatReferenz('000000000000000020260000018')).toBe('00 00000 00000 00002 02600 00018')
    expect(formatReferenz('RF422026000001')).toBe('RF42 2026 0000 01')
    expect(formatReferenz('00 00000 00000 00002 02600 00018')).toBe(
      '00 00000 00000 00002 02600 00018',
    )
  })

  it('zeigt Zeitpunkte in Schweizer Zeit', () => {
    expect(formatZeitpunkt('2026-09-30T08:15:00Z')).toBe('30.09.2026, 10:15')
  })
})
