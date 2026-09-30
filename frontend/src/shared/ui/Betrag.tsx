import { cn } from '@/shared/lib/cn'
import { formatBetrag } from '@/shared/lib/format'

/**
 * Jeder Betrag der Anwendung: rechtsbündig, mit gleich breiten Ziffern und Schweizer
 * Tausendertrennzeichen, damit sich Beträge spaltenweise vergleichen lassen.
 */
export function Betrag({
  betrag,
  waehrung,
  className,
}: {
  betrag: number
  waehrung: string
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-block text-right whitespace-nowrap tabular-nums',
        betrag < 0 && 'text-status-ueberfaellig',
        className,
      )}
    >
      {formatBetrag(betrag, waehrung)}
    </span>
  )
}
