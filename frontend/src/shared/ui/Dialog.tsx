import { useEffect, useId, useRef, type ReactNode } from 'react'

/** Modale Rückfrage auf Basis des nativen dialog-Elements, etwa vor Freigabe oder Storno. */
export function Dialog({
  offen,
  titel,
  children,
  aktionen,
  onSchliessen,
}: {
  offen: boolean
  titel: string
  children: ReactNode
  aktionen: ReactNode
  onSchliessen: () => void
}) {
  const fenster = useRef<HTMLDialogElement>(null)
  const titelId = useId()

  useEffect(() => {
    const dialog = fenster.current
    if (!dialog) {
      return
    }
    if (offen && !dialog.open) {
      dialog.showModal()
    } else if (!offen && dialog.open) {
      dialog.close()
    }
  }, [offen])

  return (
    <dialog
      ref={fenster}
      aria-labelledby={titelId}
      onClose={onSchliessen}
      className="m-auto w-full max-w-md rounded-lg border border-slate-200 bg-white p-0 text-slate-900 shadow-xl backdrop:bg-slate-900/25"
    >
      {offen && (
        <div className="p-5">
          <h2 id={titelId} className="text-base font-semibold">
            {titel}
          </h2>
          <div className="mt-2 text-sm text-slate-600">{children}</div>
          <div className="mt-5 flex justify-end gap-2">{aktionen}</div>
        </div>
      )}
    </dialog>
  )
}
