import type { RechnungDtoStatus } from '@/shared/api/generiert/modell'
import { Badge, type BadgeVariante } from './Badge'

const darstellung: Record<RechnungDtoStatus, { text: string; variante: BadgeVariante }> = {
  ENTWURF: { text: 'Entwurf', variante: 'neutral' },
  OFFEN: { text: 'Offen', variante: 'offen' },
  TEILBEZAHLT: { text: 'Teilbezahlt', variante: 'teilbezahlt' },
  BEZAHLT: { text: 'Bezahlt', variante: 'bezahlt' },
  STORNIERT: { text: 'Storniert', variante: 'neutral' },
}

/** Status mit fester Farbe und immer mit Text; überfällig ergänzt das Backend. */
export function RechnungsstatusBadge({
  status,
  ueberfaellig = false,
}: {
  status: RechnungDtoStatus
  ueberfaellig?: boolean
}) {
  if (ueberfaellig) {
    return <Badge variante="ueberfaellig">Überfällig</Badge>
  }
  const { text, variante } = darstellung[status]
  return (
    <Badge variante={variante} className={status === 'STORNIERT' ? 'line-through' : undefined}>
      {text}
    </Badge>
  )
}
