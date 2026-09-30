import type { ComponentProps } from 'react'
import { Link } from 'react-router'
import { cn } from '@/shared/lib/cn'
import { buttonKlassen, type ButtonBasis } from './button-klassen'

type ButtonProps = ButtonBasis & Omit<ComponentProps<'button'>, 'className'>

export function Button({ variante, groesse, className, type = 'button', ...props }: ButtonProps) {
  return (
    <button type={type} className={buttonKlassen({ variante, groesse, className })} {...props} />
  )
}

type LinkButtonProps = ButtonBasis & Omit<ComponentProps<typeof Link>, 'className'>

export function LinkButton({ variante, groesse, className, ...props }: LinkButtonProps) {
  return <Link className={buttonKlassen({ variante, groesse, className })} {...props} />
}

/** Button nur mit Symbol; die Beschriftung für Screenreader ist Pflicht. */
type IconButtonProps = ButtonProps & { 'aria-label': string }

export function IconButton({ className, groesse = 'md', ...props }: IconButtonProps) {
  return (
    <Button
      groesse={groesse}
      className={cn(groesse === 'sm' ? 'w-8 px-0' : 'w-9 px-0', className)}
      {...props}
    />
  )
}
