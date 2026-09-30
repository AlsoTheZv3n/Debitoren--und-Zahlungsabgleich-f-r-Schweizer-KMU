import { z } from 'zod'
import {
  kundeAnlegenBodyEmailMax,
  kundeAnlegenBodyHausnummerMax,
  kundeAnlegenBodyKundennummerMax,
  kundeAnlegenBodyNameMax,
  kundeAnlegenBodyOrtMax,
  kundeAnlegenBodyPlzMax,
  kundeAnlegenBodyStrasseMax,
  kundeAnlegenBodyZahlungsfristTageMax,
  kundeAnlegenBodyZahlungsfristTageMin,
} from '@/shared/api/generiert/zod/kunden/kunden.zod'
import type { KundeAenderungDto, KundeDto, KundeEingabeDto } from '@/shared/api/generiert/modell'

/**
 * Formularregeln für Kunden. Die Grenzen stammen aus der generierten API-Beschreibung, damit Frontend
 * und Backend dasselbe prüfen; leere optionale Felder bleiben im Formular leere Texte.
 */
const text = (max: number) => z.string().trim().max(max)
const pflicht = (max: number) => z.string().trim().min(1, 'Pflichtfeld').max(max)

export const kundeSchema = z.object({
  kundennummer: text(kundeAnlegenBodyKundennummerMax),
  name: pflicht(kundeAnlegenBodyNameMax),
  strasse: text(kundeAnlegenBodyStrasseMax),
  hausnummer: text(kundeAnlegenBodyHausnummerMax),
  plz: pflicht(kundeAnlegenBodyPlzMax),
  ort: pflicht(kundeAnlegenBodyOrtMax),
  land: z
    .string()
    .trim()
    .regex(/^[A-Z]{2}$/, 'Ländercode mit zwei Grossbuchstaben, etwa CH'),
  email: z.union([
    z.literal(''),
    z.email('Keine gültige E-Mail-Adresse').max(kundeAnlegenBodyEmailMax),
  ]),
  zahlungsfristTage: z
    .number({ error: 'Anzahl Tage angeben' })
    .int('Ganze Tage angeben')
    .min(kundeAnlegenBodyZahlungsfristTageMin)
    .max(kundeAnlegenBodyZahlungsfristTageMax),
})

export type KundeFormularWerte = z.input<typeof kundeSchema>
export type KundeFormularAusgabe = z.output<typeof kundeSchema>

export const LEERER_KUNDE: KundeFormularWerte = {
  kundennummer: '',
  name: '',
  strasse: '',
  hausnummer: '',
  plz: '',
  ort: '',
  land: 'CH',
  email: '',
  zahlungsfristTage: 30,
}

export function alsFormularwerte(kunde: KundeDto): KundeFormularWerte {
  return {
    kundennummer: kunde.kundennummer,
    name: kunde.name,
    strasse: kunde.strasse ?? '',
    hausnummer: kunde.hausnummer ?? '',
    plz: kunde.plz,
    ort: kunde.ort,
    land: kunde.land,
    email: kunde.email ?? '',
    zahlungsfristTage: kunde.zahlungsfristTage,
  }
}

const leerAlsFehlend = (wert: string) => (wert === '' ? undefined : wert)

export function alsEingabe(werte: KundeFormularAusgabe): KundeEingabeDto {
  return {
    kundennummer: leerAlsFehlend(werte.kundennummer),
    name: werte.name,
    strasse: leerAlsFehlend(werte.strasse),
    hausnummer: leerAlsFehlend(werte.hausnummer),
    plz: werte.plz,
    ort: werte.ort,
    land: werte.land,
    email: leerAlsFehlend(werte.email),
    zahlungsfristTage: werte.zahlungsfristTage,
  }
}

/** Die Kundennummer bleibt; sie steht bereits auf Rechnungen. */
export function alsAenderung(werte: KundeFormularAusgabe, version: number): KundeAenderungDto {
  const eingabe = alsEingabe(werte)
  return {
    name: eingabe.name,
    strasse: eingabe.strasse,
    hausnummer: eingabe.hausnummer,
    plz: eingabe.plz,
    ort: eingabe.ort,
    land: eingabe.land,
    email: eingabe.email,
    zahlungsfristTage: eingabe.zahlungsfristTage,
    version,
  }
}
