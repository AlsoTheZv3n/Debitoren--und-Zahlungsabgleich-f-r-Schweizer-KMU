import '@fontsource-variable/inter'
import './index.css'
import './shared/lib/zod-deutsch'
import { UserManager, WebStorageStateStore } from 'oidc-client-ts'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from 'react-oidc-context'
import { createBrowserRouter } from 'react-router'
import { FesteAnmeldung } from './app/anmeldung/FesteAnmeldung'
import { OidcAnmeldung } from './app/anmeldung/OidcAnmeldung'
import { Anwendung } from './app/Anwendung'
import { erstelleQueryClient } from './app/query-client'
import { routen } from './app/routen'
import { anmeldeZustand, zielNachAnmeldung } from './shared/lib/anmeldung'
import { setzeTokenQuelle } from './shared/lib/http'
import { ladeKonfiguration } from './shared/lib/konfiguration'
import { Vollbildmeldung } from './shared/ui/Vollbildmeldung'

const wurzelElement = document.getElementById('root')
if (!wurzelElement) {
  throw new Error('Element #root fehlt in index.html')
}
const wurzel = createRoot(wurzelElement)

if (import.meta.env.MODE === 'mock') {
  // `pnpm dev:mock`: MSW beantwortet die API im Browser, angemeldet ist ein fester Demo-Admin.
  const { starteMocks } = await import('./mocks/browser')
  await starteMocks()
  wurzel.render(
    <StrictMode>
      <FesteAnmeldung>
        <Anwendung router={createBrowserRouter(routen)} queryClient={erstelleQueryClient()} />
      </FesteAnmeldung>
    </StrictMode>,
  )
} else {
  try {
    const konfiguration = await ladeKonfiguration()
    const userManager = new UserManager({
      authority: konfiguration.oidc.authority,
      client_id: konfiguration.oidc.clientId,
      redirect_uri: `${window.location.origin}/`,
      post_logout_redirect_uri: `${window.location.origin}/`,
      scope: 'openid profile email',
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    })
    setzeTokenQuelle(async () => (await userManager.getUser())?.access_token ?? null)

    const router = createBrowserRouter(routen)
    const queryClient = erstelleQueryClient(() => {
      void userManager.signinRedirect({ state: anmeldeZustand() })
    })

    wurzel.render(
      <StrictMode>
        <AuthProvider
          userManager={userManager}
          onSigninCallback={(benutzer) => {
            void router.navigate(zielNachAnmeldung(benutzer?.state), { replace: true })
          }}
        >
          <OidcAnmeldung>
            <Anwendung router={router} queryClient={queryClient} />
          </OidcAnmeldung>
        </AuthProvider>
      </StrictMode>,
    )
  } catch (fehler) {
    wurzel.render(
      <Vollbildmeldung titel="Die Anwendung konnte nicht starten">
        {fehler instanceof Error ? fehler.message : String(fehler)}
      </Vollbildmeldung>,
    )
  }
}
