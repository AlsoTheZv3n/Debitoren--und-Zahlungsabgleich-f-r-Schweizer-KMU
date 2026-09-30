import { useEffect, useRef } from 'react'
import { Outlet } from 'react-router'
import { useAnmeldung } from '@/shared/lib/anmeldung'
import { Button } from '@/shared/ui/Button'
import { Vollbildmeldung } from '@/shared/ui/Vollbildmeldung'

/** Lässt nur angemeldete Benutzer in die Anwendung und leitet alle anderen zu Keycloak. */
export function Anmeldeschranke() {
  const anmeldung = useAnmeldung()
  const weitergeleitet = useRef(false)

  useEffect(() => {
    if (anmeldung.status === 'abgemeldet' && !weitergeleitet.current) {
      weitergeleitet.current = true
      anmeldung.anmelden()
    }
  }, [anmeldung])

  if (anmeldung.status === 'fehler') {
    return (
      <Vollbildmeldung
        titel="Anmeldung fehlgeschlagen"
        aktion={<Button onClick={anmeldung.anmelden}>Erneut anmelden</Button>}
      >
        {anmeldung.fehler ?? 'Die Anmeldung bei Keycloak hat nicht geklappt.'}
      </Vollbildmeldung>
    )
  }
  if (anmeldung.status !== 'angemeldet') {
    return <Vollbildmeldung titel="Anmeldung wird geprüft" laedt />
  }
  return <Outlet />
}
