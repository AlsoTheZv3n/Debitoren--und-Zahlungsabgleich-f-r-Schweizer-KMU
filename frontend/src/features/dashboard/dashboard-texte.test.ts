import { describe, expect, it } from 'vitest'
import { achsenmarken, achsenwert, monatKurz } from './dashboard-texte'

describe('Achsen der Dashboard-Diagramme', () => {
  it('wählt runde Schritte von 1, 2 oder 5 mal einer Zehnerpotenz', () => {
    expect(achsenmarken(28_000)).toEqual([0, 10_000, 20_000, 30_000])
    expect(achsenmarken(67_000)).toEqual([0, 20_000, 40_000, 60_000, 80_000])
    expect(achsenmarken(950)).toEqual([0, 500, 1000])
    expect(achsenmarken(40_000)).toEqual([0, 10_000, 20_000, 30_000, 40_000])
    expect(achsenmarken(0)).toEqual([0, 1])
  })

  it('schreibt Werte und Monate schweizerisch', () => {
    expect(achsenwert(150_000)).toBe('150’000')
    expect(monatKurz('2026-09')).toMatch(/^Sept?\.? 26$/)
  })
})
