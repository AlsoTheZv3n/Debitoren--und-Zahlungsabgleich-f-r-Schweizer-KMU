import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderAnwendung } from '@/test/render-anwendung'

describe('Anmeldeschranke', () => {
  it('leitet nicht angemeldete Benutzer genau einmal zur Anmeldung', async () => {
    const anmelden = vi.fn()
    renderAnwendung({ anmeldung: { status: 'abgemeldet', anmelden } })

    expect(await screen.findByText('Anmeldung wird geprüft')).toBeInTheDocument()
    expect(anmelden).toHaveBeenCalledOnce()
    expect(screen.queryByRole('navigation', { name: 'Hauptnavigation' })).toBeNull()
  })

  it('wartet, solange die Anmeldung läuft', () => {
    const anmelden = vi.fn()
    renderAnwendung({ anmeldung: { status: 'laedt', anmelden } })

    expect(screen.getByText('Anmeldung wird geprüft')).toBeInTheDocument()
    expect(anmelden).not.toHaveBeenCalled()
  })

  it('bietet nach einem Fehler eine neue Anmeldung an', async () => {
    const anmelden = vi.fn()
    const { nutzer } = renderAnwendung({
      anmeldung: { status: 'fehler', fehler: 'Keycloak nicht erreichbar', anmelden },
    })

    expect(screen.getByRole('heading', { name: 'Anmeldung fehlgeschlagen' })).toBeInTheDocument()
    expect(screen.getByText('Keycloak nicht erreichbar')).toBeInTheDocument()
    await nutzer.click(screen.getByRole('button', { name: 'Erneut anmelden' }))
    expect(anmelden).toHaveBeenCalledOnce()
  })
})
