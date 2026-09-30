import type { AuditEintragDtoObjektart } from '@/shared/api/generiert/modell'

/** Wie die Objekte des Audit-Logs in der Oberfläche heissen. */
export const OBJEKTART: Record<AuditEintragDtoObjektart, string> = {
  RECHNUNG: 'Rechnung',
  ZUORDNUNG: 'Zahlungseingang',
  MAHNUNG: 'Mahnung',
  MAHNSTUFE: 'Mahnstufe',
  KUNDE: 'Kunde',
  MANDANT: 'Mandant',
  MWST_SATZ: 'MWST-Satz',
}
