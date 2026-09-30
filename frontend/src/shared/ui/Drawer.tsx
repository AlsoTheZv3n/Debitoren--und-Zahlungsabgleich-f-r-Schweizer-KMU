import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import { IconButton } from './Button'

/**
 * Seitliches Detailfenster auf Basis des nativen dialog-Elements: Fokus bleibt im Fenster, Escape
 * schliesst, die Liste dahinter bleibt sichtbar. Offen ist es, solange es angezeigt wird; die Route
 * steuert das.
 */
export function Drawer({
  titel,
  untertitel,
  onSchliessen,
  children,
  fusszeile,
}: {
  titel: ReactNode
  untertitel?: ReactNode
  onSchliessen: () => void
  children: ReactNode
  fusszeile?: ReactNode
}) {
  const fenster = useRef<HTMLDialogElement>(null)
  const titelId = useId()

  // Kein close() beim Abbauen: Das Entfernen aus dem DOM beendet den modalen Zustand ohnehin, und ein
  // close() im StrictMode würde ein verspätetes close-Ereignis auslösen, das den Drawer schliesst.
  useEffect(() => {
    const dialog = fenster.current
    if (dialog && !dialog.open) {
      dialog.showModal()
    }
  }, [])

  return (
    <dialog
      ref={fenster}
      aria-labelledby={titelId}
      onClose={onSchliessen}
      onClick={(ereignis) => {
        // Ein Klick auf den abgedunkelten Hintergrund trifft das dialog-Element selbst.
        if (ereignis.target === fenster.current) {
          onSchliessen()
        }
      }}
      className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-dvh w-full max-w-xl flex-col border-l border-slate-200 bg-white p-0 text-slate-900 shadow-xl backdrop:bg-slate-900/25 open:flex"
    >
      <header className="flex items-start gap-3 border-b border-slate-200 px-5 py-4">
        <div className="min-w-0 flex-1">
          <h2 id={titelId} className="truncate text-base font-semibold">
            {titel}
          </h2>
          {untertitel && <p className="mt-0.5 text-sm text-slate-500">{untertitel}</p>}
        </div>
        <IconButton variante="dezent" groesse="sm" aria-label="Schliessen" onClick={onSchliessen}>
          <X aria-hidden className="size-4" />
        </IconButton>
      </header>
      <div className="flex-1 overflow-y-auto px-5 py-5">{children}</div>
      {fusszeile && (
        <footer className="flex justify-end gap-2 border-t border-slate-200 px-5 py-3">
          {fusszeile}
        </footer>
      )}
    </dialog>
  )
}
