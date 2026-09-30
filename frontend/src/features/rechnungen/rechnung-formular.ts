import { z } from 'zod'
import type { KundeWahl } from '@/features/kunden/KundenAuswahl'
import {
  EntwurfDtoWaehrung,
  EntwurfspositionDtoMwstCode,
  type EntwurfDto,
  type EntwurfspositionDto,
  type RechnungDto,
  type VorschauAnfrageDto,
} from '@/shared/api/generiert/modell'
import {
  entwurfAnlegenBodyBemerkungMax,
  entwurfAnlegenBodyPositionenItemBeschreibungMax,
  entwurfAnlegenBodyPositionenItemEinheitMax,
  entwurfAnlegenBodyPositionenMax,
  entwurfAnlegenBodyZahlungsfristTageMax,
} from '@/shared/api/generiert/zod/rechnungen/rechnungen.zod'
import { alsEingabe, alsZahl, nachkommastellen } from '@/shared/lib/zahlen'

/**
 * Formularregeln für Rechnungsentwürfe. Mengen und Preise bleiben im Formular Text, damit Schweizer
 * Schreibweisen wie «1’250.50» durchgehen; ans Backend gehen sie als Zahl. Die Stellen entsprechen den
 * Spalten der Datenbank: Menge mit drei, Preis mit zwei Nachkommastellen.
 */
function dezimalzahl({
  vorkomma,
  nachkomma,
  nullErlaubt,
}: {
  vorkomma: number
  nachkomma: number
  nullErlaubt: boolean
}) {
  return z
    .string()
    .trim()
    .min(1, { error: 'Pflichtfeld', abort: true })
    .refine((text) => alsZahl(text) !== null, { error: 'Keine gültige Zahl', abort: true })
    .refine((text) => nachkommastellen(text) <= nachkomma, {
      error:
        nachkomma === 2 ? 'Höchstens zwei Nachkommastellen' : 'Höchstens drei Nachkommastellen',
      abort: true,
    })
    .refine((text) => (alsZahl(text) ?? 0) < 10 ** vorkomma, { error: 'Zu gross', abort: true })
    .refine((text) => nullErlaubt || (alsZahl(text) ?? 0) > 0, { error: 'Grösser als null' })
}

export const positionSchema = z.object({
  beschreibung: z
    .string()
    .trim()
    .min(1, 'Pflichtfeld')
    .max(entwurfAnlegenBodyPositionenItemBeschreibungMax),
  menge: dezimalzahl({ vorkomma: 9, nachkomma: 3, nullErlaubt: false }),
  einheit: z.string().trim().max(entwurfAnlegenBodyPositionenItemEinheitMax),
  einzelpreis: dezimalzahl({ vorkomma: 10, nachkomma: 2, nullErlaubt: true }),
  mwstCode: z.enum(EntwurfspositionDtoMwstCode),
})

export const rechnungSchema = z.object({
  kunde: z.custom<KundeWahl | null>().transform((kunde, kontext) => {
    if (!kunde) {
      kontext.addIssue({ code: 'custom', message: 'Kunde wählen' })
      return z.NEVER
    }
    return kunde
  }),
  rechnungsdatum: z.iso.date({ error: 'Datum angeben' }),
  zahlungsfristTage: z
    .string()
    .trim()
    .refine(
      (text) =>
        text === '' ||
        (/^\d+$/.test(text) && Number(text) <= entwurfAnlegenBodyZahlungsfristTageMax),
      `Ganze Tage von 0 bis ${entwurfAnlegenBodyZahlungsfristTageMax}`,
    ),
  waehrung: z.enum(EntwurfDtoWaehrung),
  bemerkung: z.string().trim().max(entwurfAnlegenBodyBemerkungMax),
  positionen: z
    .array(positionSchema)
    .min(1, 'Mindestens eine Position')
    .max(
      entwurfAnlegenBodyPositionenMax,
      `Höchstens ${entwurfAnlegenBodyPositionenMax} Positionen`,
    ),
})

export type RechnungFormularWerte = z.input<typeof rechnungSchema>
export type RechnungFormularAusgabe = z.output<typeof rechnungSchema>
export type PositionWerte = z.input<typeof positionSchema>

export const LEERE_POSITION: PositionWerte = {
  beschreibung: '',
  menge: '1',
  einheit: '',
  einzelpreis: '',
  mwstCode: 'NORMAL',
}

export function neueRechnung(
  rechnungsdatum: string,
  kunde: KundeWahl | null,
): RechnungFormularWerte {
  return {
    kunde,
    rechnungsdatum,
    zahlungsfristTage: '',
    waehrung: 'CHF',
    bemerkung: '',
    positionen: [{ ...LEERE_POSITION }],
  }
}

export function alsFormularwerte(rechnung: RechnungDto): RechnungFormularWerte {
  return {
    kunde: { id: rechnung.kundeId, name: rechnung.empfaenger.name },
    rechnungsdatum: rechnung.rechnungsdatum,
    zahlungsfristTage: String(rechnung.zahlungsfristTage),
    waehrung: rechnung.waehrung,
    bemerkung: rechnung.bemerkung ?? '',
    positionen: rechnung.positionen.map((position) => ({
      beschreibung: position.beschreibung,
      menge: alsEingabe(position.menge),
      einheit: position.einheit ?? '',
      einzelpreis: alsEingabe(position.einzelpreis, 2),
      mwstCode: position.mwstCode,
    })),
  }
}

const leerAlsFehlend = (text: string) => (text === '' ? undefined : text)

function alsEntwurfsposition(position: z.output<typeof positionSchema>): EntwurfspositionDto {
  return {
    beschreibung: position.beschreibung,
    menge: alsZahl(position.menge) ?? 0,
    einheit: leerAlsFehlend(position.einheit),
    einzelpreis: alsZahl(position.einzelpreis) ?? 0,
    mwstCode: position.mwstCode,
  }
}

export function alsEntwurf(werte: RechnungFormularAusgabe): EntwurfDto {
  return {
    kundeId: werte.kunde.id,
    rechnungsdatum: werte.rechnungsdatum,
    zahlungsfristTage: werte.zahlungsfristTage === '' ? undefined : Number(werte.zahlungsfristTage),
    waehrung: werte.waehrung,
    bemerkung: leerAlsFehlend(werte.bemerkung),
    positionen: werte.positionen.map(alsEntwurfsposition),
  }
}

/** Anfrage für die Live-Vorschau samt Zuordnung ihrer Positionen zu den Zeilen im Formular. */
export interface Vorschauanfrage {
  anfrage: VorschauAnfrageDto
  zeilen: number[]
}

/**
 * Baut die Vorschau schon während der Eingabe: Zeilen mit ungültiger Menge oder ungültigem Preis
 * fehlen darin, eine fehlende Beschreibung stört die Summen nicht. Ohne gültiges Datum gibt es keine
 * Vorschau.
 */
export function alsVorschauanfrage(
  werte: Pick<
    RechnungFormularWerte,
    'kunde' | 'rechnungsdatum' | 'zahlungsfristTage' | 'waehrung' | 'positionen'
  >,
): Vorschauanfrage | null {
  if (!z.iso.date().safeParse(werte.rechnungsdatum).success) {
    return null
  }
  const positionen: EntwurfspositionDto[] = []
  const zeilen: number[] = []
  werte.positionen.forEach((position, zeile) => {
    const geprueft = positionSchema.safeParse({
      ...position,
      beschreibung: position.beschreibung.trim() || 'Vorschau',
    })
    if (geprueft.success) {
      positionen.push(alsEntwurfsposition(geprueft.data))
      zeilen.push(zeile)
    }
  })
  const frist = werte.zahlungsfristTage.trim()
  const fristGueltig =
    /^\d+$/.test(frist) && Number(frist) <= entwurfAnlegenBodyZahlungsfristTageMax
  return {
    anfrage: {
      kundeId: werte.kunde?.id,
      rechnungsdatum: werte.rechnungsdatum,
      zahlungsfristTage: fristGueltig ? Number(frist) : undefined,
      waehrung: werte.waehrung,
      positionen,
    },
    zeilen,
  }
}

/**
 * Feldname aus einem Fehler des Backends als Pfad im Formular: «positionen[1].menge» wird zu
 * «positionen.1.menge», die Kunden-Id zum Feld «kunde».
 */
export function alsFormularpfad(feld: string): string {
  const pfad = feld.replace(/^entwurf\./, '').replace(/\[(\d+)\]/g, '.$1')
  return pfad === 'kundeId' ? 'kunde' : pfad
}
