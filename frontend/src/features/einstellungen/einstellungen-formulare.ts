import { z } from 'zod'
import type {
  MahnstufeDto,
  MahnstufenAnfrageDto,
  MandantAenderungDto,
  MandantDetailDto,
} from '@/shared/api/generiert/modell'
import {
  mahnstufenFestlegenBodyStufenItemBezeichnungMax,
  mahnstufenFestlegenBodyStufenItemFristTageMax,
  mahnstufenFestlegenBodyStufenItemGebuehrMax,
  mahnstufenFestlegenBodyStufenItemMindestabstandTageMax,
  mahnstufenFestlegenBodyStufenItemTageNachFaelligkeitMax,
  mahnstufenFestlegenBodyStufenItemTextvorlageMax,
  mahnstufenFestlegenBodyStufenMax,
} from '@/shared/api/generiert/zod/mahnstufen/mahnstufen.zod'
import {
  mandantAendernBodyEmailMax,
  mandantAendernBodyHausnummerMax,
  mandantAendernBodyMwstNummerMax,
  mandantAendernBodyNameMax,
  mandantAendernBodyOrtMax,
  mandantAendernBodyPlzMax,
  mandantAendernBodyStrasseMax,
} from '@/shared/api/generiert/zod/mandanten/mandanten.zod'
import { alsEingabe, alsZahl, nachkommastellen } from '@/shared/lib/zahlen'

/**
 * Formularregeln der Einstellungen. Die Grenzen stammen aus der generierten API-Beschreibung; IBAN-Prüfziffer und
 * MWST-Nummer prüft das Backend, das Formular zeigt dessen Meldung beim Feld.
 */
const text = (max: number) => z.string().trim().max(max)
const pflicht = (max: number) => z.string().trim().min(1, 'Pflichtfeld').max(max)
const leerAlsFehlend = (wert: string) => (wert === '' ? undefined : wert)

const iban = z
  .string()
  .trim()
  .transform((wert) => wert.replace(/\s/g, '').toUpperCase())
  .refine(
    (wert) => /^(CH|LI)\d{2}[0-9A-Z]{17}$/.test(wert),
    'Schweizer IBAN mit 21 Zeichen, etwa CH58 0079 1123 0008 8901 2',
  )

export const mandantSchema = z.object({
  name: pflicht(mandantAendernBodyNameMax),
  strasse: text(mandantAendernBodyStrasseMax),
  hausnummer: text(mandantAendernBodyHausnummerMax),
  plz: pflicht(mandantAendernBodyPlzMax),
  ort: pflicht(mandantAendernBodyOrtMax),
  land: z
    .string()
    .trim()
    .regex(/^[A-Z]{2}$/, 'Ländercode mit zwei Grossbuchstaben, etwa CH'),
  iban,
  qrIban: z.union([z.literal(''), iban]),
  mwstNummer: text(mandantAendernBodyMwstNummerMax),
  email: z.union([
    z.literal(''),
    z.email('Keine gültige E-Mail-Adresse').max(mandantAendernBodyEmailMax),
  ]),
  rundungFuenfRappen: z.boolean(),
})

export type MandantWerte = z.input<typeof mandantSchema>
export type MandantAusgabe = z.output<typeof mandantSchema>

export function mandantAlsWerte(mandant: MandantDetailDto): MandantWerte {
  return {
    name: mandant.name,
    strasse: mandant.strasse ?? '',
    hausnummer: mandant.hausnummer ?? '',
    plz: mandant.plz,
    ort: mandant.ort,
    land: mandant.land,
    iban: mandant.iban,
    qrIban: mandant.qrIban ?? '',
    mwstNummer: mandant.mwstNummer ?? '',
    email: mandant.email ?? '',
    rundungFuenfRappen: mandant.rundungFuenfRappen,
  }
}

export function mandantAlsAenderung(werte: MandantAusgabe, version: number): MandantAenderungDto {
  return {
    name: werte.name,
    strasse: leerAlsFehlend(werte.strasse),
    hausnummer: leerAlsFehlend(werte.hausnummer),
    plz: werte.plz,
    ort: werte.ort,
    land: werte.land,
    iban: werte.iban,
    qrIban: leerAlsFehlend(werte.qrIban),
    mwstNummer: leerAlsFehlend(werte.mwstNummer),
    email: leerAlsFehlend(werte.email),
    rundungFuenfRappen: werte.rundungFuenfRappen,
    version,
  }
}

export const PLATZHALTER = ['{rechnungsnummer}', '{faelligAm}', '{offen}', '{frist}', '{gebuehr}']

const tage = (min: number, max: number) =>
  z
    .number({ error: 'Anzahl Tage angeben' })
    .int('Ganze Tage angeben')
    .min(min, `Mindestens ${String(min)}`)
    .max(max, `Höchstens ${String(max)}`)

const gebuehr = z
  .string()
  .trim()
  .min(1, { error: 'Pflichtfeld', abort: true })
  .refine((wert) => alsZahl(wert) !== null, { error: 'Keine gültige Zahl', abort: true })
  .refine((wert) => nachkommastellen(wert) <= 2, 'Höchstens zwei Nachkommastellen')
  .refine(
    (wert) => (alsZahl(wert) ?? 0) <= mahnstufenFestlegenBodyStufenItemGebuehrMax,
    `Höchstens ${String(mahnstufenFestlegenBodyStufenItemGebuehrMax)}`,
  )

const stufe = z.object({
  bezeichnung: pflicht(mahnstufenFestlegenBodyStufenItemBezeichnungMax),
  tageNachFaelligkeit: tage(1, mahnstufenFestlegenBodyStufenItemTageNachFaelligkeitMax),
  mindestabstandTage: tage(0, mahnstufenFestlegenBodyStufenItemMindestabstandTageMax),
  fristTage: tage(1, mahnstufenFestlegenBodyStufenItemFristTageMax),
  gebuehr,
  textvorlage: pflicht(mahnstufenFestlegenBodyStufenItemTextvorlageMax).refine(
    (vorlage) =>
      [...vorlage.matchAll(/\{[^{}]*\}/g)].every((treffer) => PLATZHALTER.includes(treffer[0])),
    `Unbekannter Platzhalter; erlaubt sind ${PLATZHALTER.join(', ')}`,
  ),
})

export const mahnstufenSchema = z
  .object({
    stufen: z.array(stufe).min(1, 'Mindestens eine Stufe').max(mahnstufenFestlegenBodyStufenMax),
  })
  .superRefine((werte, kontext) => {
    werte.stufen.forEach((eintrag, index) => {
      const vorher = werte.stufen[index - 1]
      if (vorher && eintrag.tageNachFaelligkeit <= vorher.tageNachFaelligkeit) {
        kontext.addIssue({
          code: 'custom',
          path: ['stufen', index, 'tageNachFaelligkeit'],
          message: `Muss später sein als Stufe ${String(index)} (${String(vorher.tageNachFaelligkeit)} Tage)`,
        })
      }
    })
  })

export type MahnstufenWerte = z.input<typeof mahnstufenSchema>
export type MahnstufenAusgabe = z.output<typeof mahnstufenSchema>

export const MAX_STUFEN = mahnstufenFestlegenBodyStufenMax

export function mahnstufenAlsWerte(stufen: MahnstufeDto[]): MahnstufenWerte {
  return {
    stufen: stufen.map((eintrag) => ({
      bezeichnung: eintrag.bezeichnung,
      tageNachFaelligkeit: eintrag.tageNachFaelligkeit,
      mindestabstandTage: eintrag.mindestabstandTage,
      fristTage: eintrag.fristTage,
      gebuehr: alsEingabe(eintrag.gebuehr, 2),
      textvorlage: eintrag.textvorlage,
    })),
  }
}

/** Eine weitere Stufe nach der letzten, mit dem üblichen Abstand von zwei Wochen. */
export function neueStufe(werte: MahnstufenWerte): MahnstufenWerte['stufen'][number] {
  const letzte = werte.stufen[werte.stufen.length - 1]
  return {
    bezeichnung: `${String(werte.stufen.length)}. Mahnung`,
    tageNachFaelligkeit: (letzte?.tageNachFaelligkeit ?? 0) + 15,
    mindestabstandTage: 14,
    fristTage: 10,
    gebuehr: '0.00',
    textvorlage:
      'Die Rechnung {rechnungsnummer} ist weiterhin offen. Bitte überweisen Sie {offen} bis am {frist}.',
  }
}

/**
 * Die Stufe an Position n ersetzt die bisherige Stufe n; ihre Version kommt deshalb von der Position, nicht von
 * der Stufe, die jemand verschoben oder gelöscht hat.
 */
export function mahnstufenAlsAnfrage(
  werte: MahnstufenAusgabe,
  bisher: MahnstufeDto[],
): MahnstufenAnfrageDto {
  return {
    stufen: werte.stufen.map((eintrag, index) => ({
      version: bisher[index]?.version,
      bezeichnung: eintrag.bezeichnung,
      tageNachFaelligkeit: eintrag.tageNachFaelligkeit,
      mindestabstandTage: index === 0 ? 0 : eintrag.mindestabstandTage,
      fristTage: eintrag.fristTage,
      gebuehr: alsZahl(eintrag.gebuehr) ?? 0,
      textvorlage: eintrag.textvorlage,
    })),
  }
}
