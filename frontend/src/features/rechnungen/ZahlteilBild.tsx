import { useQuery } from '@tanstack/react-query'
import { getRechnungZahlteilQueryKey, rechnungZahlteil } from '@/shared/api/generiert/endpunkte'
import { cn } from '@/shared/lib/cn'
import { svgAlsBildquelle } from '@/shared/lib/download'
import { fehlertext } from '@/shared/lib/http'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'

/**
 * QR-Zahlteil als Bild. Er wird als data:-Adresse eingebunden, weil die CSP keine blob:-Bilder
 * erlaubt; das SVG selbst ist vom Rest der Seite getrennt.
 */
export function ZahlteilBild({
  svg,
  beschriftung,
  className,
}: {
  svg: string
  beschriftung: string
  className?: string
}) {
  return (
    <img
      src={svgAlsBildquelle(svg)}
      alt={beschriftung}
      className={cn('w-full rounded-lg border border-slate-200 bg-white', className)}
    />
  )
}

/** Zahlteil einer gespeicherten Rechnung, so wie er auf dem PDF steht. */
export function GespeicherterZahlteil({ id, beschriftung }: { id: string; beschriftung: string }) {
  const zahlteil = useQuery({
    queryKey: getRechnungZahlteilQueryKey(id),
    queryFn: async ({ signal }) => (await rechnungZahlteil(id, { signal })).text(),
  })

  if (zahlteil.isPending) {
    return <Ladeanzeige text="Zahlteil wird erstellt…" />
  }
  if (zahlteil.isError) {
    return <Meldung>{fehlertext(zahlteil.error)}</Meldung>
  }
  return <ZahlteilBild svg={zahlteil.data} beschriftung={beschriftung} />
}
