import { useId, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

/** Was ein Eingabeelement braucht, damit Beschriftung, Hinweis und Fehler zugänglich verknüpft sind. */
export interface FeldSteuerung {
  id: string
  'aria-invalid'?: true
  'aria-describedby'?: string
}

export function Feld({
  label,
  hinweis,
  fehler,
  optional = false,
  labelVersteckt = false,
  className,
  children,
}: {
  label: string
  /** Nur für Screenreader, etwa in Tabellen, deren Spaltenkopf die Beschriftung zeigt. */
  labelVersteckt?: boolean
  hinweis?: ReactNode
  fehler?: string
  optional?: boolean
  className?: string
  children: (steuerung: FeldSteuerung) => ReactNode
}) {
  const id = useId()
  const hinweisId = `${id}-hinweis`
  const fehlerId = `${id}-fehler`
  const beschreibung = [hinweis ? hinweisId : null, fehler ? fehlerId : null]
    .filter(Boolean)
    .join(' ')

  return (
    <div className={cn('space-y-1', className)}>
      <label
        htmlFor={id}
        className={cn('block text-sm font-medium text-slate-700', labelVersteckt && 'sr-only')}
      >
        {label}
        {optional && <span className="font-normal text-slate-500"> (optional)</span>}
      </label>
      {children({
        id,
        'aria-invalid': fehler ? true : undefined,
        'aria-describedby': beschreibung || undefined,
      })}
      {hinweis && (
        <p id={hinweisId} className="text-xs text-slate-500">
          {hinweis}
        </p>
      )}
      {fehler && (
        <p id={fehlerId} className="text-xs text-red-700">
          {fehler}
        </p>
      )}
    </div>
  )
}
