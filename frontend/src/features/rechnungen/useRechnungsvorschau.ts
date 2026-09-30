import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getRechnungVorschauUrl, rechnungVorschau } from '@/shared/api/generiert/endpunkte'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import type { Vorschauanfrage } from './rechnung-formular'

/**
 * Summen und Zahlteil des Entwurfs, während er entsteht. Die Anfrage geht erst, wenn die Eingabe kurz
 * ruht; bis die neue Antwort da ist, bleibt die alte stehen. Gerechnet wird nur im Backend.
 */
export function useRechnungsvorschau(vorschau: Vorschauanfrage | null) {
  const schluessel = useEntprellt(JSON.stringify(vorschau), 400)

  return useQuery({
    queryKey: [getRechnungVorschauUrl(), schluessel],
    queryFn: async ({ signal }) => {
      const { anfrage, zeilen } = JSON.parse(schluessel) as Vorschauanfrage
      return { ...(await rechnungVorschau(anfrage, { signal })), zeilen }
    },
    enabled: schluessel !== 'null',
    placeholderData: keepPreviousData,
    staleTime: Infinity,
    retry: false,
  })
}
