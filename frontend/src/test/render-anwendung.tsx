import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { HttpHandler } from 'msw'
import { createMemoryRouter } from 'react-router'
import { FesteAnmeldung } from '@/app/anmeldung/FesteAnmeldung'
import { Anwendung } from '@/app/Anwendung'
import { erstelleQueryClient } from '@/app/query-client'
import { routen } from '@/app/routen'
import { DEMO_MANDANTEN, demoBenutzer } from '@/mocks/demo-daten'
import { getAktuellerBenutzerMockHandler } from '@/shared/api/generiert/endpunkte/benutzer/benutzer.msw'
import { getMandantenAuflistenMockHandler } from '@/shared/api/generiert/endpunkte/mandanten/mandanten.msw'
import type { BenutzerDto, MandantDto } from '@/shared/api/generiert/modell'
import type { Anmeldung } from '@/shared/lib/anmeldung'
import { server } from './server'

/** Rendert die ganze Anwendung mit Router, Query-Client und fester Anmeldung; MSW spielt das Backend. */
export function renderAnwendung({
  pfad = '/',
  benutzer = demoBenutzer(),
  mandanten = DEMO_MANDANTEN,
  anmeldung = {},
  handler = [],
}: {
  pfad?: string
  benutzer?: BenutzerDto
  mandanten?: MandantDto[]
  anmeldung?: Partial<Anmeldung>
  /** Zusätzliche Antworten; sie gehen den Standardantworten vor. */
  handler?: HttpHandler[]
} = {}) {
  server.use(
    ...handler,
    getAktuellerBenutzerMockHandler(benutzer),
    getMandantenAuflistenMockHandler(mandanten),
  )
  const router = createMemoryRouter(routen, { initialEntries: [pfad] })
  const nutzer = userEvent.setup()
  render(
    <FesteAnmeldung {...anmeldung}>
      <Anwendung router={router} queryClient={erstelleQueryClient()} />
    </FesteAnmeldung>,
  )
  return { router, nutzer }
}
