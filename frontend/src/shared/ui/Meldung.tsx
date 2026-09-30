import { CircleAlert, CircleCheck, Info } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

type Art = 'fehler' | 'erfolg' | 'hinweis'

const stil: Record<Art, string> = {
  fehler: 'border-red-200 bg-red-50 text-red-800',
  erfolg: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  hinweis: 'border-slate-200 bg-slate-50 text-slate-700',
}

const zeichen = { fehler: CircleAlert, erfolg: CircleCheck, hinweis: Info }

/** Rückmeldung im Inhalt; Fehler werden von Screenreadern sofort vorgelesen. */
export function Meldung({
  art = 'fehler',
  className,
  children,
}: {
  art?: Art
  className?: string
  children: ReactNode
}) {
  const Zeichen = zeichen[art]
  return (
    <div
      role={art === 'fehler' ? 'alert' : 'status'}
      className={cn('flex gap-2 rounded-lg border px-3 py-2 text-sm', stil[art], className)}
    >
      <Zeichen aria-hidden className="mt-0.5 size-4 shrink-0" />
      <div className="min-w-0">{children}</div>
    </div>
  )
}
