import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { Building2, Check, ChevronsUpDown } from 'lucide-react'
import { useMandant } from '@/shared/lib/mandant'
import { buttonKlassen } from '@/shared/ui/button-klassen'

/** Mandantenwahl in der Kopfzeile; bei nur einem Mandanten eine reine Anzeige. */
export function MandantWahl() {
  const { mandanten, aktiv, waehlen } = useMandant()

  if (mandanten.length === 1) {
    return (
      <p className="flex items-center gap-2 px-2 text-sm text-slate-700">
        <Building2 aria-hidden className="size-4 text-slate-400" />
        <span className="sr-only">Mandant:</span>
        <span className="max-w-48 truncate font-medium">{aktiv.name}</span>
      </p>
    )
  }

  return (
    // modal={false}: keine Scroll-Sperre, die ein <style>-Element einfügen und die CSP verletzen würde.
    <DropdownMenu.Root modal={false}>
      <DropdownMenu.Trigger
        className={buttonKlassen({ variante: 'dezent', className: 'max-w-64' })}
        aria-label={`Mandant wählen, aktuell ${aktiv.name}`}
      >
        <Building2 aria-hidden className="size-4 text-slate-400" />
        <span className="truncate">{aktiv.name}</span>
        <ChevronsUpDown aria-hidden className="size-4 text-slate-400" />
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={6}
          className="z-20 min-w-64 rounded-lg border border-slate-200 bg-white p-1 text-sm shadow-sm"
        >
          <DropdownMenu.Label className="px-2 py-1.5 text-xs font-medium text-slate-500">
            Mandant
          </DropdownMenu.Label>
          <DropdownMenu.RadioGroup value={aktiv.id} onValueChange={waehlen}>
            {mandanten.map((mandant) => (
              <DropdownMenu.RadioItem
                key={mandant.id}
                value={mandant.id}
                className="flex cursor-default items-center gap-2 rounded-md py-1.5 pr-2 pl-7 outline-none select-none data-highlighted:bg-slate-100"
              >
                <DropdownMenu.ItemIndicator className="absolute left-3">
                  <Check aria-hidden className="size-4 text-brand-600" />
                </DropdownMenu.ItemIndicator>
                <span className="flex flex-col">
                  <span className="font-medium text-slate-900">{mandant.name}</span>
                  <span className="text-xs text-slate-500">
                    {mandant.ort} · {mandant.referenzart === 'QR' ? 'QR-Referenz' : 'SCOR-Referenz'}
                  </span>
                </span>
              </DropdownMenu.RadioItem>
            ))}
          </DropdownMenu.RadioGroup>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
