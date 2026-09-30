import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { BERNER_WERKSTATT, demoBenutzer, SEELAND_GARTENBAU } from '@/mocks/demo-daten'
import { aktuellerMandant } from '@/shared/lib/http'
import { renderAnwendung } from '@/test/render-anwendung'

const hauptnavigation = () => screen.getByRole('navigation', { name: 'Hauptnavigation' })

describe('Anwendung', () => {
  it('zeigt dem Admin Sitzung, alle Rollen und die Einstellungen', async () => {
    renderAnwendung()

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(await screen.findByText('Guten Tag, Andrea Meier')).toBeInTheDocument()
    const sitzung = screen.getByRole('region', { name: 'Ihre Sitzung' })
    expect(within(sitzung).getByText('Admin')).toBeInTheDocument()
    expect(within(sitzung).getByText('Buchhaltung')).toBeInTheDocument()
    expect(within(sitzung).getByText('Leser')).toBeInTheDocument()
    expect(within(sitzung).getByText('Berner Werkstatt AG')).toBeInTheDocument()
    expect(within(hauptnavigation()).getByRole('link', { name: 'Einstellungen' })).toBeVisible()
  })

  it('blendet die Einstellungen für Leser aus und sperrt die Seite', async () => {
    renderAnwendung({
      pfad: '/einstellungen',
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    expect(await screen.findByRole('heading', { name: 'Kein Zugriff' })).toBeInTheDocument()
    expect(screen.getByText('Diese Seite ist der Rolle Admin vorbehalten')).toBeInTheDocument()
    expect(within(hauptnavigation()).queryByRole('link', { name: 'Einstellungen' })).toBeNull()
    expect(within(hauptnavigation()).getByRole('link', { name: 'Rechnungen' })).toBeVisible()
  })

  it('zeigt bei nur einem Mandanten keine Auswahl', async () => {
    renderAnwendung({
      benutzer: demoBenutzer(['LESER'], [BERNER_WERKSTATT]),
      mandanten: [BERNER_WERKSTATT],
    })

    await screen.findByRole('heading', { name: 'Dashboard' })
    expect(screen.queryByRole('button', { name: /Mandant wählen/ })).toBeNull()
    expect(aktuellerMandant()).toBe(BERNER_WERKSTATT.id)
  })

  it('wechselt den Mandanten und sendet ihn bei jeder weiteren Anfrage mit', async () => {
    const { nutzer } = renderAnwendung()

    await nutzer.click(await screen.findByRole('button', { name: /Mandant wählen/ }))
    await nutzer.click(await screen.findByRole('menuitemradio', { name: /Seeland Gartenbau/ }))

    const sitzung = screen.getByRole('region', { name: 'Ihre Sitzung' })
    expect(await within(sitzung).findByText('Seeland Gartenbau GmbH')).toBeInTheDocument()
    expect(within(sitzung).getByText('SCOR-Referenz')).toBeInTheDocument()
    expect(aktuellerMandant()).toBe(SEELAND_GARTENBAU.id)
    expect(window.localStorage.getItem('debitoren.mandant')).toBe(SEELAND_GARTENBAU.id)
  })

  it('nimmt den zuletzt gewählten Mandanten wieder auf', async () => {
    window.localStorage.setItem('debitoren.mandant', SEELAND_GARTENBAU.id)
    renderAnwendung()

    const sitzung = await screen.findByRole('region', { name: 'Ihre Sitzung' })
    expect(within(sitzung).getByText('Seeland Gartenbau GmbH')).toBeInTheDocument()
  })

  it('erklärt, wenn dem Benutzer keine Rolle zugewiesen ist', async () => {
    const abmelden = vi.fn()
    const { nutzer } = renderAnwendung({
      benutzer: demoBenutzer([]),
      anmeldung: { abmelden },
      handler: [
        http.get('*/api/mandanten', () =>
          HttpResponse.json(
            { title: 'Zugriff verweigert', status: 403, code: 'ZUGRIFF_VERWEIGERT' },
            { status: 403, headers: { 'Content-Type': 'application/problem+json' } },
          ),
        ),
      ],
    })

    expect(await screen.findByRole('heading', { name: 'Keine Berechtigung' })).toBeInTheDocument()
    await nutzer.click(screen.getByRole('button', { name: 'Abmelden' }))
    expect(abmelden).toHaveBeenCalledOnce()
  })

  it('führt über die Brotkrumen und meldet unbekannte Seiten', async () => {
    renderAnwendung({ pfad: '/zahlungen/klaerung' })

    const brotkrumen = await screen.findByRole('navigation', { name: 'Brotkrumen' })
    expect(within(brotkrumen).getByText('Zahlungen')).toBeInTheDocument()
    expect(within(brotkrumen).getByText('Klärungsliste')).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('heading', { name: 'Klärungsliste' })).toBeInTheDocument()
  })

  it('zeigt eine Meldung für unbekannte Adressen', async () => {
    renderAnwendung({ pfad: '/gibt-es-nicht' })

    expect(await screen.findByRole('heading', { name: 'Seite nicht gefunden' })).toBeInTheDocument()
  })

  it('meldet den Benutzer ab', async () => {
    const abmelden = vi.fn()
    const { nutzer } = renderAnwendung({ anmeldung: { abmelden } })

    await nutzer.click(await screen.findByRole('button', { name: /Benutzermenü/ }))
    await nutzer.click(await screen.findByRole('menuitem', { name: 'Abmelden' }))
    await waitFor(() => {
      expect(abmelden).toHaveBeenCalledOnce()
    })
  })

  it('zählt die ungeklärten Zahlungen in der Navigation', async () => {
    renderAnwendung()

    const navigation = await screen.findByRole('navigation', { name: 'Hauptnavigation' })
    const klaerung = await within(navigation).findByRole('link', {
      name: /^Klärungsliste\s*3\s*ungeklärt$/,
    })
    expect(klaerung).toHaveAttribute('href', '/zahlungen/klaerung')
  })
})
