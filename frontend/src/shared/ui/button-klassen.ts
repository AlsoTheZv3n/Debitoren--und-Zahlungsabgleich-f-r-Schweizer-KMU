import { cn } from '@/shared/lib/cn'

export type ButtonVariante = 'primaer' | 'sekundaer' | 'dezent'
export type ButtonGroesse = 'sm' | 'md'

/** Gemeinsame Basis-Props von Button, LinkButton und IconButton; die drei sind austauschbar. */
export interface ButtonBasis {
  variante?: ButtonVariante
  groesse?: ButtonGroesse
  className?: string
}

const varianten: Record<ButtonVariante, string> = {
  primaer: 'bg-brand-600 text-white hover:bg-brand-700',
  sekundaer: 'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50',
  dezent: 'text-slate-700 hover:bg-slate-100 hover:text-slate-900',
}

const groessen: Record<ButtonGroesse, string> = {
  sm: 'h-8 gap-1.5 px-2.5 text-sm',
  md: 'h-9 gap-2 px-3.5 text-sm',
}

export function buttonKlassen({ variante = 'primaer', groesse = 'md', className }: ButtonBasis) {
  return cn(
    'inline-flex shrink-0 items-center justify-center rounded-lg font-medium whitespace-nowrap transition-colors',
    'focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2 focus-visible:outline-none',
    'disabled:pointer-events-none disabled:opacity-50',
    varianten[variante],
    groessen[groesse],
    className,
  )
}
