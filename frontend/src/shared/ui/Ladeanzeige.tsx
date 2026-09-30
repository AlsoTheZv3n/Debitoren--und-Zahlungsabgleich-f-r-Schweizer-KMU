import { LoaderCircle } from 'lucide-react'
import { cn } from '@/shared/lib/cn'

export function Ladeanzeige({
  text = 'Wird geladen…',
  className,
}: {
  text?: string
  className?: string
}) {
  return (
    <div
      role="status"
      className={cn('flex items-center gap-2 py-8 text-sm text-slate-500', className)}
    >
      <LoaderCircle aria-hidden className="size-4 animate-spin" />
      {text}
    </div>
  )
}
