import { ChevronRight } from 'lucide-react'
import { Fragment } from 'react'
import { useMatches } from 'react-router'
import { cn } from '@/shared/lib/cn'
import type { RoutenHandle } from '../routen-handle'

/** Pfad der aktuellen Seite, gebildet aus den Titeln der Routen, etwa «Zahlungen › Klärungsliste». */
export function Brotkrumen({ className }: { className?: string }) {
  const titel = useMatches()
    .map((treffer) => (treffer.handle as RoutenHandle | undefined)?.titel)
    .filter((t): t is string => Boolean(t))

  if (titel.length === 0) {
    return null
  }
  return (
    <nav aria-label="Brotkrumen" className={cn('min-w-0', className)}>
      <ol className="flex items-center gap-1.5 text-sm text-slate-500">
        {titel.map((eintrag, index) => (
          <Fragment key={eintrag}>
            {index > 0 && <ChevronRight aria-hidden className="size-3.5 shrink-0 text-slate-400" />}
            <li
              className={cn('truncate', index === titel.length - 1 && 'font-medium text-slate-900')}
              aria-current={index === titel.length - 1 ? 'page' : undefined}
            >
              {eintrag}
            </li>
          </Fragment>
        ))}
      </ol>
    </nav>
  )
}
