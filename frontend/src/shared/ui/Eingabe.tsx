import type { ComponentProps } from 'react'
import { cn } from '@/shared/lib/cn'

const feld =
  'w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900 placeholder:text-slate-500 ' +
  'focus-visible:border-brand-600 focus-visible:ring-2 focus-visible:ring-brand-600/25 focus-visible:outline-none ' +
  'disabled:bg-slate-50 disabled:text-slate-500 aria-invalid:border-red-600 aria-invalid:ring-red-600/20'

export function Eingabe({ className, ...props }: ComponentProps<'input'>) {
  return <input className={cn(feld, 'h-9', className)} {...props} />
}

export function Textbereich({ className, ...props }: ComponentProps<'textarea'>) {
  return <textarea className={cn(feld, 'min-h-20 py-2', className)} {...props} />
}

export function Auswahl({ className, ...props }: ComponentProps<'select'>) {
  return <select className={cn(feld, 'h-9 pr-8', className)} {...props} />
}

/** Checkbox; `unbestimmt` zeigt eine Teilauswahl an, etwa im Tabellenkopf. */
export function Ankreuzfeld({
  className,
  unbestimmt = false,
  ...props
}: Omit<ComponentProps<'input'>, 'type'> & { unbestimmt?: boolean }) {
  return (
    <input
      type="checkbox"
      ref={(element) => {
        if (element) {
          element.indeterminate = unbestimmt
        }
      }}
      className={cn(
        'size-4 rounded border-slate-300 accent-brand-600 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2 focus-visible:outline-none',
        className,
      )}
      {...props}
    />
  )
}
