import type { ReactNode } from 'react'

export function Seitenkopf({
  titel,
  beschreibung,
  aktionen,
}: {
  titel: string
  beschreibung?: ReactNode
  aktionen?: ReactNode
}) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-xl font-semibold tracking-tight text-slate-900">{titel}</h1>
        {beschreibung && <p className="mt-1 text-sm text-slate-500">{beschreibung}</p>}
      </div>
      {aktionen && <div className="flex items-center gap-2">{aktionen}</div>}
    </header>
  )
}
