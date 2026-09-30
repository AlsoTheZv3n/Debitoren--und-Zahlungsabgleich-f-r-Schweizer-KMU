import { SearchX } from 'lucide-react'
import { LinkButton } from '@/shared/ui/Button'
import { LeererZustand } from '@/shared/ui/LeererZustand'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

export function NichtGefundenSeite() {
  return (
    <>
      <Seitenkopf titel="Seite nicht gefunden" />
      <LeererZustand
        symbol={SearchX}
        titel="Diese Adresse gibt es nicht"
        hinweis={
          <LinkButton to="/" variante="sekundaer">
            Zur Übersicht
          </LinkButton>
        }
      >
        Vielleicht ist der Link veraltet oder falsch abgetippt.
      </LeererZustand>
    </>
  )
}
