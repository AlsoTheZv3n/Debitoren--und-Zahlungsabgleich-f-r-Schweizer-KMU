import { keepPreviousData, useQueryClient } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  useRechnungenSuchen,
  useZahlungVorschlaege,
  useZahlungZuordnen,
  useZahlungZuordnungen,
  useZuordnungAufheben,
} from '@/shared/api/generiert/endpunkte'
import type { ZahlungDto } from '@/shared/api/generiert/modell'
import { formatBetrag, formatDatum, formatReferenz } from '@/shared/lib/format'
import { fehlertext, invalidieren } from '@/shared/lib/http'
import { useRollen } from '@/shared/lib/rollen'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { alsZahl, nachkommastellen } from '@/shared/lib/zahlen'
import { Badge } from '@/shared/ui/Badge'
import { Betrag } from '@/shared/ui/Betrag'
import { Button } from '@/shared/ui/Button'
import { Eingabe } from '@/shared/ui/Eingabe'
import { Feld } from '@/shared/ui/Feld'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { KLAERUNGSGRUND, ZUORDNUNGSART } from './zahlungstexte'

interface Rechnungswahl {
  rechnungId: string
  rechnungsnummer: string
  kundeName: string
  faelligAm: string
  offen: number
  waehrung: string
}

/**
 * Die gewählte Zahlung: was die Bank meldet, was schon zugeordnet ist, was die Strategien vorschlagen, und
 * eine Suche nach weiteren offenen Rechnungen. Zugeordnet wird mit einem Klick.
 */
export interface Rueckmeldung {
  art: 'erfolg' | 'fehler'
  text: string
}

export function ZahlungDetail({
  zahlung,
  onRueckmeldung,
}: {
  zahlung: ZahlungDto
  /** Die Seite zeigt die Rückmeldung; nach einer vollständigen Zuordnung ist diese Zahlung nicht mehr gewählt. */
  onRueckmeldung: (rueckmeldung: Rueckmeldung | null) => void
}) {
  const queryClient = useQueryClient()
  const { hatRolle } = useRollen()
  const darfZuordnen = hatRolle('BUCHHALTUNG')
  const [betragEingabe, setBetragEingabe] = useState('')
  const vorschlaege = useZahlungVorschlaege(zahlung.id)
  const zuordnungen = useZahlungZuordnungen(zahlung.id)
  const zuordnen = useZahlungZuordnen()
  const aufheben = useZuordnungAufheben()

  const betrag = betragEingabe.trim() === '' ? undefined : alsZahl(betragEingabe)
  const betragFehler =
    betrag === null || (betrag !== undefined && nachkommastellen(betragEingabe) > 2)
      ? 'Betrag mit höchstens zwei Nachkommastellen, etwa 120.50'
      : undefined

  const neuLaden = async () => {
    await invalidieren(queryClient, '/api/zahlungen')
    await invalidieren(queryClient, '/api/rechnungen')
    await invalidieren(queryClient, '/api/zuordnungen')
  }

  const zuordnenZu = async (rechnung: Rechnungswahl) => {
    if (betragFehler) {
      return
    }
    onRueckmeldung(null)
    try {
      const ergebnis = await zuordnen.mutateAsync({
        id: zahlung.id,
        data: {
          version: zahlung.version,
          rechnungId: rechnung.rechnungId,
          betrag: betrag ?? undefined,
        },
      })
      onRueckmeldung({
        art: 'erfolg',
        text:
          ergebnis.rest > 0
            ? `Rechnung ${rechnung.rechnungsnummer} zugeordnet; ${formatBetrag(ergebnis.rest, ergebnis.waehrung)} bleiben in der Klärung.`
            : `Zahlung von ${zahlung.auftraggeber ?? 'unbekannt'} der Rechnung ${rechnung.rechnungsnummer} zugeordnet.`,
      })
      setBetragEingabe('')
    } catch (fehler) {
      onRueckmeldung({ art: 'fehler', text: fehlertext(fehler) })
    }
    await neuLaden()
  }

  const zuordnungAufheben = async (id: string) => {
    onRueckmeldung(null)
    try {
      await aufheben.mutateAsync({ id })
      onRueckmeldung({ art: 'erfolg', text: 'Zuordnung aufgehoben.' })
    } catch (fehler) {
      onRueckmeldung({ art: 'fehler', text: fehlertext(fehler) })
    }
    await neuLaden()
  }

  const beschaeftigt = zuordnen.isPending || aufheben.isPending

  return (
    <section
      aria-labelledby="gewaehlte-zahlung"
      className="space-y-5 rounded-lg border border-slate-200 bg-white p-4 lg:sticky lg:top-4"
    >
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 id="gewaehlte-zahlung" className="text-base font-semibold text-slate-900">
            {zahlung.auftraggeber ?? 'Unbekannter Auftraggeber'}
          </h2>
          {zahlung.klaerungsgrund && (
            <Badge variante="klaerung" className="mt-1">
              {KLAERUNGSGRUND[zahlung.klaerungsgrund]}
            </Badge>
          )}
        </div>
        <div className="text-right">
          <Betrag
            betrag={zahlung.rest}
            waehrung={zahlung.waehrung}
            className="text-lg font-semibold"
          />
          <p className="text-xs text-slate-500">
            {zahlung.rest === zahlung.betrag
              ? 'nicht zugeordnet'
              : `Rest von ${formatBetrag(zahlung.betrag, zahlung.waehrung)}`}
          </p>
        </div>
      </header>

      <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1.5 text-sm">
        <dt className="text-slate-500">Valuta</dt>
        <dd className="tabular-nums">{formatDatum(zahlung.valuta)}</dd>
        {zahlung.referenz && (
          <>
            <dt className="text-slate-500">Referenz</dt>
            <dd className="font-mono text-xs leading-5">{formatReferenz(zahlung.referenz)}</dd>
          </>
        )}
        {zahlung.mitteilung && (
          <>
            <dt className="text-slate-500">Mitteilung</dt>
            <dd>{zahlung.mitteilung}</dd>
          </>
        )}
        {zahlung.auftraggeberIban && (
          <>
            <dt className="text-slate-500">Konto</dt>
            <dd className="font-mono text-xs leading-5">{zahlung.auftraggeberIban}</dd>
          </>
        )}
        <dt className="text-slate-500">Bankreferenz</dt>
        <dd className="text-xs leading-5 break-all text-slate-600">{zahlung.bankReferenz}</dd>
      </dl>

      {(zuordnungen.data?.length ?? 0) > 0 && (
        <section aria-labelledby="bereits-zugeordnet">
          <h3 id="bereits-zugeordnet" className="mb-2 text-sm font-semibold text-slate-900">
            Bereits zugeordnet
          </h3>
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
            {zuordnungen.data?.map((zuordnung) => (
              <li
                key={zuordnung.id}
                className="flex flex-wrap items-center gap-3 px-3 py-2 text-sm"
              >
                <Link
                  to={`/rechnungen/${zuordnung.rechnungId}`}
                  className="font-medium text-brand-700 hover:underline"
                >
                  {zuordnung.rechnungsnummer ?? 'Rechnung'}
                </Link>
                <span className="text-slate-500">{ZUORDNUNGSART[zuordnung.art]}</span>
                <Betrag
                  className="ml-auto"
                  betrag={zuordnung.betrag}
                  waehrung={zuordnung.waehrung}
                />
                {darfZuordnen && (
                  <Button
                    groesse="sm"
                    variante="dezent"
                    disabled={beschaeftigt}
                    onClick={() => void zuordnungAufheben(zuordnung.id)}
                  >
                    Aufheben
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      {darfZuordnen && (
        <Feld
          label="Betrag"
          optional
          hinweis={`Leer lassen, um so viel wie möglich zuzuordnen; offen sind ${formatBetrag(zahlung.rest, zahlung.waehrung)}.`}
          fehler={betragFehler}
          className="max-w-xs"
        >
          {(steuerung) => (
            <Eingabe
              {...steuerung}
              inputMode="decimal"
              placeholder={zahlung.rest.toFixed(2)}
              className="text-right tabular-nums"
              value={betragEingabe}
              onChange={(ereignis) => {
                setBetragEingabe(ereignis.target.value)
              }}
            />
          )}
        </Feld>
      )}

      <section aria-labelledby="vorschlaege">
        <h3 id="vorschlaege" className="mb-2 text-sm font-semibold text-slate-900">
          Vorschläge
        </h3>
        {vorschlaege.isPending ? (
          <Ladeanzeige className="py-2" />
        ) : vorschlaege.isError ? (
          <Meldung>{fehlertext(vorschlaege.error)}</Meldung>
        ) : vorschlaege.data.length === 0 ? (
          <p className="text-sm text-slate-500">
            Keine Rechnung passt eindeutig; unten lässt sich suchen.
          </p>
        ) : (
          <ul className="space-y-2">
            {vorschlaege.data.map((vorschlag) => (
              <li key={vorschlag.rechnungId}>
                <Rechnungskarte
                  rechnung={vorschlag}
                  begruendung={vorschlag.begruendung}
                  darfZuordnen={darfZuordnen}
                  beschaeftigt={beschaeftigt}
                  onZuordnen={() => void zuordnenZu(vorschlag)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>

      <RechnungSuchen
        waehrung={zahlung.waehrung}
        darfZuordnen={darfZuordnen}
        beschaeftigt={beschaeftigt}
        onZuordnen={(rechnung) => void zuordnenZu(rechnung)}
      />
    </section>
  )
}

function Rechnungskarte({
  rechnung,
  begruendung,
  darfZuordnen,
  beschaeftigt,
  onZuordnen,
}: {
  rechnung: Rechnungswahl
  begruendung?: string
  darfZuordnen: boolean
  beschaeftigt: boolean
  onZuordnen: () => void
}) {
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-lg border border-slate-200 px-3 py-2 text-sm">
      <div className="min-w-0 flex-1">
        <p>
          <Link
            to={`/rechnungen/${rechnung.rechnungId}`}
            className="font-medium text-brand-700 hover:underline"
          >
            {rechnung.rechnungsnummer}
          </Link>{' '}
          <span className="text-slate-900">{rechnung.kundeName}</span>
        </p>
        <p className="text-xs text-slate-500">
          offen {formatBetrag(rechnung.offen, rechnung.waehrung)} · fällig{' '}
          {formatDatum(rechnung.faelligAm)}
          {begruendung && ` · ${begruendung}`}
        </p>
      </div>
      {darfZuordnen && (
        <Button
          groesse="sm"
          disabled={beschaeftigt}
          aria-label={`Rechnung ${rechnung.rechnungsnummer} zuordnen`}
          onClick={onZuordnen}
        >
          Zuordnen
        </Button>
      )}
    </div>
  )
}

/** Offene Rechnungen nach Nummer oder Kunde, für Zahlungen ohne passenden Vorschlag. */
function RechnungSuchen({
  waehrung,
  darfZuordnen,
  beschaeftigt,
  onZuordnen,
}: {
  waehrung: string
  darfZuordnen: boolean
  beschaeftigt: boolean
  onZuordnen: (rechnung: Rechnungswahl) => void
}) {
  const [eingabe, setEingabe] = useState('')
  const suche = useEntprellt(eingabe.trim(), 300)
  const treffer = useRechnungenSuchen(
    { offen: true, suche, groesse: 5, sortierung: 'FAELLIGKEIT_AUFSTEIGEND' },
    { query: { enabled: suche !== '', placeholderData: keepPreviousData } },
  )

  return (
    <section aria-labelledby="rechnung-suchen">
      <h3 id="rechnung-suchen" className="mb-2 text-sm font-semibold text-slate-900">
        Rechnung suchen
      </h3>
      <label className="relative block">
        <span className="sr-only">Offene Rechnung suchen</span>
        <Search
          aria-hidden
          className="pointer-events-none absolute top-2.5 left-3 size-4 text-slate-400"
        />
        <Eingabe
          type="search"
          value={eingabe}
          placeholder="Rechnungsnummer oder Kunde"
          className="pl-9"
          onChange={(ereignis) => {
            setEingabe(ereignis.target.value)
          }}
        />
      </label>
      {suche !== '' && treffer.data && (
        <ul className="mt-2 space-y-2">
          {treffer.data.inhalt.length === 0 && (
            <li className="text-sm text-slate-500">Keine offene Rechnung passt zu «{suche}».</li>
          )}
          {treffer.data.inhalt.map((rechnung) => (
            <li key={rechnung.id}>
              <Rechnungskarte
                rechnung={{
                  rechnungId: rechnung.id,
                  rechnungsnummer: rechnung.rechnungsnummer ?? '',
                  kundeName: rechnung.kundeName,
                  faelligAm: rechnung.faelligAm,
                  offen: rechnung.offen,
                  waehrung: rechnung.waehrung,
                }}
                begruendung={rechnung.waehrung === waehrung ? undefined : `in ${rechnung.waehrung}`}
                darfZuordnen={darfZuordnen}
                beschaeftigt={beschaeftigt}
                onZuordnen={() => {
                  onZuordnen({
                    rechnungId: rechnung.id,
                    rechnungsnummer: rechnung.rechnungsnummer ?? '',
                    kundeName: rechnung.kundeName,
                    faelligAm: rechnung.faelligAm,
                    offen: rechnung.offen,
                    waehrung: rechnung.waehrung,
                  })
                }}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
