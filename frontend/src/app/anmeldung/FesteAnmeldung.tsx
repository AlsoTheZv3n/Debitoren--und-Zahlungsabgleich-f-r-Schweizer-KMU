import { useMemo, type ReactNode } from 'react'
import { AnmeldungKontext, type Anmeldung } from '@/shared/lib/anmeldung'

const nichts = () => undefined

/** Anmeldung ohne Keycloak für Tests und `pnpm dev:mock`; wer angemeldet ist, bestimmt /api/me. */
export function FesteAnmeldung({
  children,
  status = 'angemeldet',
  fehler,
  anmelden = nichts,
  abmelden = nichts,
}: Partial<Anmeldung> & { children: ReactNode }) {
  const anmeldung = useMemo<Anmeldung>(
    () => ({ status, fehler, anmelden, abmelden }),
    [status, fehler, anmelden, abmelden],
  )
  return <AnmeldungKontext value={anmeldung}>{children}</AnmeldungKontext>
}
