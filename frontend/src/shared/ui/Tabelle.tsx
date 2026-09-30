import type { ComponentProps } from 'react'
import { cn } from '@/shared/lib/cn'

/**
 * Tabellenbausteine mit einheitlichem Stil: dicht, Linien statt Schatten, Beträge rechtsbündig. Die
 * Zeilen und Spalten liefert TanStack Table; diese Bausteine stellen nur dar.
 */
export function Tabelle({ className, ...props }: ComponentProps<'table'>) {
  return (
    // relative: Auch absolut positionierte Inhalte wie sr-only-Beschriftungen scrollen mit der Tabelle,
    // statt die Seite zu verbreitern.
    <div className="relative overflow-x-auto">
      <table className={cn('w-full border-collapse text-sm', className)} {...props} />
    </div>
  )
}

export function Kopfzelle({ className, ...props }: ComponentProps<'th'>) {
  return (
    <th
      scope="col"
      className={cn(
        'border-b border-slate-200 bg-slate-50 px-3 py-2 text-left text-xs font-medium whitespace-nowrap text-slate-500',
        className,
      )}
      {...props}
    />
  )
}

export function Zelle({ className, ...props }: ComponentProps<'td'>) {
  return (
    <td
      className={cn('border-b border-slate-100 px-3 py-2 align-top text-slate-900', className)}
      {...props}
    />
  )
}

export function Zeile({ className, ...props }: ComponentProps<'tr'>) {
  return <tr className={cn('hover:bg-slate-50', className)} {...props} />
}
