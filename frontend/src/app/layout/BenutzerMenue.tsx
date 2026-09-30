import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { LogOut } from 'lucide-react'
import { useAktuellerBenutzer } from '@/shared/api/generiert/endpunkte'
import { useAnmeldung } from '@/shared/lib/anmeldung'
import { initialen } from '@/shared/lib/format'
import { ROLLEN, useRollen } from '@/shared/lib/rollen'
import { Badge } from '@/shared/ui/Badge'
import { buttonKlassen } from '@/shared/ui/button-klassen'

export function BenutzerMenue() {
  const { data: benutzer } = useAktuellerBenutzer()
  const { rollen, hauptrolle } = useRollen()
  const { abmelden } = useAnmeldung()

  if (!benutzer) {
    return null
  }
  return (
    <DropdownMenu.Root modal={false}>
      <DropdownMenu.Trigger
        className={buttonKlassen({ variante: 'dezent', className: 'gap-2.5 pr-2 pl-1.5' })}
        aria-label={`Benutzermenü von ${benutzer.anzeigename}`}
      >
        <span
          aria-hidden
          className="grid size-7 place-items-center rounded-full bg-brand-600 text-xs font-semibold text-white"
        >
          {initialen(benutzer.anzeigename)}
        </span>
        <span className="hidden flex-col items-start leading-tight sm:flex">
          <span className="text-sm font-medium text-slate-900">{benutzer.anzeigename}</span>
          {hauptrolle && (
            <span className="text-xs font-normal text-slate-500">{ROLLEN[hauptrolle].name}</span>
          )}
        </span>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={6}
          className="z-20 w-72 rounded-lg border border-slate-200 bg-white p-1 text-sm shadow-sm"
        >
          <div className="px-2 py-2">
            <p className="font-medium text-slate-900">{benutzer.anzeigename}</p>
            <p className="truncate text-xs text-slate-500">
              {benutzer.email ?? benutzer.benutzername}
            </p>
            <p className="mt-2 flex flex-wrap gap-1">
              {rollen.map((rolle) => (
                <Badge key={rolle} variante="brand">
                  {ROLLEN[rolle].name}
                </Badge>
              ))}
            </p>
          </div>
          <DropdownMenu.Separator className="my-1 h-px bg-slate-200" />
          <DropdownMenu.Item
            onSelect={abmelden}
            className="flex cursor-default items-center gap-2 rounded-md px-2 py-1.5 text-slate-700 outline-none select-none data-highlighted:bg-slate-100 data-highlighted:text-slate-900"
          >
            <LogOut aria-hidden className="size-4" />
            Abmelden
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
