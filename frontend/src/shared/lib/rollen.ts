import { useAktuellerBenutzer } from '@/shared/api/generiert/endpunkte'
import type { BenutzerDtoRollenItem } from '@/shared/api/generiert/modell'

export type Rolle = BenutzerDtoRollenItem

/** Bezeichnung und Rechte je Rolle, wie in der Projektplanung festgelegt. */
export const ROLLEN: Record<Rolle, { name: string; darf: string }> = {
  ADMIN: {
    name: 'Admin',
    darf: 'Mandant, Benutzer, Mahnstufen und MWST-Sätze konfigurieren, zusätzlich alles der Buchhaltung',
  },
  BUCHHALTUNG: {
    name: 'Buchhaltung',
    darf: 'Kunden und Rechnungen pflegen, Dateien importieren, Zahlungen zuordnen, Mahnungen freigeben',
  },
  LESER: {
    name: 'Leser',
    darf: 'Alles ansehen, nichts ändern',
  },
}

/**
 * Wirksame Rollen des angemeldeten Benutzers. Das Backend löst die Hierarchie auf (Admin schliesst
 * Buchhaltung und Leser ein) und prüft jede Aktion selbst; die Oberfläche blendet nur aus.
 */
export function useRollen() {
  const benutzer = useAktuellerBenutzer()
  const rollen = benutzer.data?.rollen ?? []
  return {
    rollen,
    laedt: benutzer.isPending,
    hatRolle: (rolle: Rolle) => rollen.includes(rolle),
    /** Die höchste Rolle, für die Anzeige im Benutzermenü. */
    hauptrolle: rollen[0],
  }
}
