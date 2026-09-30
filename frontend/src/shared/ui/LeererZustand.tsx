import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

/** Platzhalter für eine leere Liste oder einen Bereich, der noch keine Daten hat. */
export function LeererZustand({
  symbol: Zeichen,
  titel,
  children,
  hinweis,
}: {
  symbol: LucideIcon
  titel: string
  children?: ReactNode
  hinweis?: ReactNode
}) {
  return (
    <section className="rounded-lg border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
      <Zeichen aria-hidden className="mx-auto size-8 text-slate-400" strokeWidth={1.5} />
      <h2 className="mt-3 text-sm font-semibold text-slate-900">{titel}</h2>
      {children && (
        <div className="mx-auto mt-1 max-w-prose text-sm text-slate-500">{children}</div>
      )}
      {hinweis && <div className="mt-4">{hinweis}</div>}
    </section>
  )
}
