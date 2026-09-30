import type { MwstSatzDtoCode } from '@/shared/api/generiert/modell'

/** Bezeichnungen der MWST-Codes; die Sätze selbst liefert das Backend je Stichtag. */
export const MWST_BEZEICHNUNG: Record<MwstSatzDtoCode, string> = {
  NORMAL: 'Normalsatz',
  REDUZIERT: 'Reduzierter Satz',
  BEHERBERGUNG: 'Beherbergung',
  OHNE: 'Ohne MWST',
}

export const MWST_CODES = Object.keys(MWST_BEZEICHNUNG) as MwstSatzDtoCode[]
