import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export type BadgeVariante =
  'neutral' | 'brand' | 'offen' | 'teilbezahlt' | 'bezahlt' | 'ueberfaellig' | 'klaerung'

const varianten: Record<BadgeVariante, string> = {
  neutral: 'bg-slate-100 text-slate-700 ring-slate-200',
  brand: 'bg-brand-50 text-brand-700 ring-brand-100',
  offen: 'bg-slate-100 text-status-offen ring-slate-200',
  teilbezahlt: 'bg-amber-50 text-amber-800 ring-amber-200',
  bezahlt: 'bg-emerald-50 text-emerald-800 ring-emerald-200',
  ueberfaellig: 'bg-red-50 text-red-700 ring-red-200',
  klaerung: 'bg-violet-50 text-violet-700 ring-violet-200',
}

/** Status nie nur über Farbe: jede Badge trägt ihren Text. */
export function Badge({
  variante = 'neutral',
  className,
  children,
}: {
  variante?: BadgeVariante
  className?: string
  children: ReactNode
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-1.5 py-0.5 text-xs font-medium ring-1 ring-inset',
        varianten[variante],
        className,
      )}
    >
      {children}
    </span>
  )
}
