import { describe, expect, it } from 'vitest'
import { zielNachAnmeldung } from './anmeldung'

describe('zielNachAnmeldung', () => {
  it('führt zurück zur ursprünglich aufgerufenen Seite', () => {
    expect(zielNachAnmeldung({ zurueck: '/zahlungen/klaerung?zahlung=4' })).toBe(
      '/zahlungen/klaerung?zahlung=4',
    )
  })

  it('erlaubt keine Umleitung auf fremde Seiten', () => {
    expect(zielNachAnmeldung({ zurueck: 'https://boese.example/' })).toBe('/')
    expect(zielNachAnmeldung({ zurueck: '//boese.example/' })).toBe('/')
  })

  it('fällt ohne gültigen Zustand auf die Übersicht zurück', () => {
    expect(zielNachAnmeldung(undefined)).toBe('/')
    expect(zielNachAnmeldung(null)).toBe('/')
    expect(zielNachAnmeldung({ zurueck: 42 })).toBe('/')
  })
})
