import type { BenutzerDto, MandantDto } from '@/shared/api/generiert/modell'

/** Dieselben Demo-Mandanten wie im Seeder des Backends und im Keycloak-Realm. */
export const BERNER_WERKSTATT: MandantDto = {
  id: '019a0000-0000-7000-8000-000000000001',
  name: 'Berner Werkstatt AG',
  ort: 'Bern',
  referenzart: 'QR',
}

export const SEELAND_GARTENBAU: MandantDto = {
  id: '019a0000-0000-7000-8000-000000000002',
  name: 'Seeland Gartenbau GmbH',
  ort: 'Nidau',
  referenzart: 'SCOR',
}

export const DEMO_MANDANTEN: MandantDto[] = [BERNER_WERKSTATT, SEELAND_GARTENBAU]

export function demoBenutzer(
  rollen: BenutzerDto['rollen'] = ['ADMIN', 'BUCHHALTUNG', 'LESER'],
  mandanten: MandantDto[] = DEMO_MANDANTEN,
): BenutzerDto {
  return {
    benutzername: 'admin',
    anzeigename: 'Andrea Meier',
    email: 'andrea.meier@debitoren.example',
    rollen,
    mandanten: mandanten.map((m) => m.id),
  }
}
