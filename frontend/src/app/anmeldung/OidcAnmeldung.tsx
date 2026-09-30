import { useMemo, type ReactNode } from 'react'
import { useAuth } from 'react-oidc-context'
import { anmeldeZustand, AnmeldungKontext, type Anmeldung } from '@/shared/lib/anmeldung'

/** Anmeldung über Keycloak: Authorization Code mit PKCE, erledigt von oidc-client-ts. */
export function OidcAnmeldung({ children }: { children: ReactNode }) {
  const auth = useAuth()

  const anmeldung = useMemo<Anmeldung>(
    () => ({
      status: auth.error
        ? 'fehler'
        : auth.isLoading || auth.activeNavigator
          ? 'laedt'
          : auth.isAuthenticated
            ? 'angemeldet'
            : 'abgemeldet',
      fehler: auth.error?.message,
      anmelden: () => {
        void auth.signinRedirect({ state: anmeldeZustand() })
      },
      abmelden: () => {
        void auth.signoutRedirect()
      },
    }),
    [auth],
  )

  return <AnmeldungKontext value={anmeldung}>{children}</AnmeldungKontext>
}
