import { BellRing } from 'lucide-react'
import { Badge } from '@/shared/ui/Badge'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

export function MahnwesenSeite() {
  return (
    <>
      <Seitenkopf titel="Mahnwesen" beschreibung="Mahnvorschläge prüfen und gesammelt freigeben." />
      <LeererZustand
        symbol={BellRing}
        titel="Keine Mahnvorschläge"
        hinweis={<Badge>Folgt in Phase 4</Badge>}
      >
        Der tägliche Mahnlauf schlägt für überfällige Rechnungen die nächste Stufe vor; versandt
        wird erst nach der Freigabe.
      </LeererZustand>
    </>
  )
}
