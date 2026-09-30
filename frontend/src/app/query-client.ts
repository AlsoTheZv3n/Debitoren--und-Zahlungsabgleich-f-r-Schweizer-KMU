import { QueryCache, QueryClient } from '@tanstack/react-query'
import { ApiFehler } from '@/shared/lib/http'

/**
 * Serverzustand lebt nur in TanStack Query. Fachliche Fehler (4xx) werden nicht wiederholt; ein 401
 * heisst, dass das Token abgelaufen ist und neu angemeldet werden muss.
 */
export function erstelleQueryClient(beiAbgelaufenerAnmeldung: () => void = () => undefined) {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (fehler) => {
        if (fehler instanceof ApiFehler && fehler.status === 401) {
          beiAbgelaufenerAnmeldung()
        }
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (anzahl, fehler) =>
          !(fehler instanceof ApiFehler && fehler.status < 500) && anzahl < 2,
      },
    },
  })
}
