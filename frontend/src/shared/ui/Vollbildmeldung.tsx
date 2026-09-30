import type { ReactNode } from 'react'
import { Ladeanzeige } from './Ladeanzeige'

/** Meldung, solange noch keine Oberfläche steht: Anmeldung, Startfehler, fehlender Mandant. */
export function Vollbildmeldung({
  titel,
  children,
  aktion,
  laedt = false,
}: {
  titel: string
  children?: ReactNode
  aktion?: ReactNode
  laedt?: boolean
}) {
  return (
    <main className="grid min-h-dvh place-items-center p-4">
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6">
        <p className="text-sm font-semibold text-brand-700">Debitoren</p>
        <h1 className="mt-1 text-lg font-semibold text-slate-900">{titel}</h1>
        {children && <div className="mt-2 text-sm text-slate-600">{children}</div>}
        {laedt && <Ladeanzeige className="py-4" />}
        {aktion && <div className="mt-5">{aktion}</div>}
      </div>
    </main>
  )
}
