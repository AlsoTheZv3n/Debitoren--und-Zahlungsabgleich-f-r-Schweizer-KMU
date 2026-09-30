import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Betrag } from './Betrag'

describe('Betrag', () => {
  it('rendert rechtsbündig mit gleich breiten Ziffern', () => {
    render(<Betrag betrag={1840} waehrung="CHF" />)

    const betrag = screen.getByText('CHF 1’840.00')
    expect(betrag).toHaveClass('text-right', 'tabular-nums', 'whitespace-nowrap')
  })

  it('hebt negative Beträge hervor', () => {
    render(<Betrag betrag={-95.5} waehrung="CHF" />)

    expect(screen.getByText(/95\.50/)).toHaveClass('text-status-ueberfaellig')
  })
})
