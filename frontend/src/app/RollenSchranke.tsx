import { ShieldAlert } from 'lucide-react'
import type { ReactNode } from 'react'
import { ROLLEN, useRollen, type Rolle } from '@/shared/lib/rollen'
import { LinkButton } from '@/shared/ui/Button'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

/** Zeigt eine Seite nur mit der nötigen Rolle. Die eigentliche Prüfung macht das Backend. */
export function RollenSchranke({ rolle, children }: { rolle: Rolle; children: ReactNode }) {
  const { laedt, hatRolle } = useRollen()

  if (laedt) {
    return <Ladeanzeige />
  }
  if (!hatRolle(rolle)) {
    return (
      <>
        <Seitenkopf titel="Kein Zugriff" />
        <LeererZustand
          symbol={ShieldAlert}
          titel={`Diese Seite ist der Rolle ${ROLLEN[rolle].name} vorbehalten`}
          hinweis={
            <LinkButton to="/" variante="sekundaer">
              Zur Übersicht
            </LinkButton>
          }
        >
          Wenn Sie hier arbeiten müssen, bitten Sie die Administration um die passende Rolle.
        </LeererZustand>
      </>
    )
  }
  return children
}
