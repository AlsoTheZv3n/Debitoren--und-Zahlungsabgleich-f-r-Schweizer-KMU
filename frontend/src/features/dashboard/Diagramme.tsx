import {
  Bar,
  BarChart,
  CartesianGrid,
  Rectangle,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type BarShapeProps,
} from 'recharts'
import type { ReactNode } from 'react'
import { useAltersstrukturLesen, useZahlungseingaengeLesen } from '@/shared/api/generiert/endpunkte'
import type { MonatseingangDto, WaehrungsstrukturDto } from '@/shared/api/generiert/modell'
import { fehlertext } from '@/shared/lib/http'
import { Betrag } from '@/shared/ui/Betrag'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { Kopfzelle, Tabelle, Zelle } from '@/shared/ui/Tabelle'
import { formatBetrag } from '@/shared/lib/format'
import { ALTERSBAND, achsenmarken, achsenwert, monatKurz } from './dashboard-texte'
import {
  ACHSE,
  ACHSENSCHRIFT,
  AKZENT,
  ALTERSSKALA,
  FLAECHE,
  GITTER,
  HOVER,
  ZURUECKTRETEND,
} from './diagrammfarben'

/**
 * Diagramme des Dashboards. Sie laden als eigener Teil nach, damit Recharts die übrige Anwendung nicht bremst.
 * Jedes Diagramm hat eine Tabelle mit denselben Werten; der Tooltip ergänzt nur.
 */

const HOEHE = 240
const achsenschrift = { fill: ACHSENSCHRIFT, fontSize: 12 }

export function AltersstrukturKarte() {
  const abfrage = useAltersstrukturLesen()
  const chf = abfrage.data?.waehrungen.find((waehrung) => waehrung.waehrung === 'CHF')
  const weitere = abfrage.data?.waehrungen.filter((waehrung) => waehrung.waehrung !== 'CHF') ?? []

  return (
    <Karte
      id="altersstruktur"
      titel="Altersstruktur der offenen Posten"
      beschreibung="Offene Beträge in CHF nach Tagen über der Fälligkeit"
    >
      {abfrage.isPending ? (
        <Ladeanzeige className="px-4" />
      ) : abfrage.isError ? (
        <Meldung className="m-4">{fehlertext(abfrage.error)}</Meldung>
      ) : (
        <>
          {chf && <Altersdiagramm struktur={chf} />}
          {weitere.map((waehrung) => (
            <p key={waehrung.waehrung} className="px-4 pb-2 text-sm text-slate-600">
              Dazu offen in {waehrung.waehrung}:{' '}
              <Betrag betrag={waehrung.total} waehrung={waehrung.waehrung} />
            </p>
          ))}
          <Tabellenansicht>
            <Tabelle>
              <thead>
                <tr>
                  <Kopfzelle>Band</Kopfzelle>
                  {abfrage.data.waehrungen.map((waehrung) => (
                    <Kopfzelle key={waehrung.waehrung} className="text-right">
                      {waehrung.waehrung}
                    </Kopfzelle>
                  ))}
                  <Kopfzelle className="text-right">Rechnungen</Kopfzelle>
                </tr>
              </thead>
              <tbody>
                {(chf?.baender ?? []).map((band, index) => (
                  <tr key={band.band}>
                    <Zelle>{ALTERSBAND[band.band]}</Zelle>
                    {abfrage.data.waehrungen.map((waehrung) => (
                      <Zelle key={waehrung.waehrung} className="text-right">
                        <Betrag
                          betrag={waehrung.baender[index]?.betrag ?? 0}
                          waehrung={waehrung.waehrung}
                        />
                      </Zelle>
                    ))}
                    <Zelle className="text-right tabular-nums">
                      {abfrage.data.waehrungen.reduce(
                        (summe, waehrung) => summe + (waehrung.baender[index]?.anzahl ?? 0),
                        0,
                      )}
                    </Zelle>
                  </tr>
                ))}
              </tbody>
            </Tabelle>
          </Tabellenansicht>
        </>
      )}
    </Karte>
  )
}

function Altersdiagramm({ struktur }: { struktur: WaehrungsstrukturDto }) {
  const daten = struktur.baender.map((band) => ({ ...band, bezeichnung: ALTERSBAND[band.band] }))
  const marken = achsenmarken(Math.max(...daten.map((band) => band.betrag)))
  return (
    <div className="px-2 pb-2">
      <ResponsiveContainer width="100%" height={HOEHE}>
        <BarChart
          data={daten}
          title="Offene Posten in CHF nach Tagen über der Fälligkeit"
          margin={{ top: 8, right: 12, bottom: 0, left: 8 }}
        >
          <CartesianGrid vertical={false} stroke={GITTER} />
          <XAxis
            dataKey="bezeichnung"
            tickLine={false}
            axisLine={{ stroke: ACHSE }}
            tick={achsenschrift}
            interval={0}
          />
          <YAxis
            tickFormatter={achsenwert}
            ticks={marken}
            domain={[0, marken[marken.length - 1] ?? 0]}
            tickLine={false}
            axisLine={false}
            tick={achsenschrift}
            width={72}
          />
          <Tooltip
            cursor={{ fill: HOVER }}
            content={({ active, payload }) => {
              const eintrag = payload[0]?.payload as (typeof daten)[number] | undefined
              return active && eintrag ? (
                <Hinweis titel={eintrag.bezeichnung}>
                  <strong className="block text-slate-900">
                    {formatBetrag(eintrag.betrag, struktur.waehrung)}
                  </strong>
                  <span className="text-slate-500">
                    {eintrag.anzahl === 1
                      ? 'eine Rechnung'
                      : `${String(eintrag.anzahl)} Rechnungen`}
                  </span>
                </Hinweis>
              ) : null
            }}
          />
          {/* Jedes Band trägt seine Stufe der Skala: je älter, desto dunkler. */}
          <Bar
            dataKey="betrag"
            name="Offen"
            maxBarSize={24}
            radius={[4, 4, 0, 0]}
            isAnimationActive={false}
            shape={(eigenschaften: BarShapeProps) => (
              <Rectangle {...eigenschaften} fill={ALTERSSKALA[eigenschaften.index] ?? AKZENT} />
            )}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

interface Monat {
  monat: string
  bezeichnung: string
  betrag: number
  automatisch: number
  uebrige: number
  anzahl: number
}

export function ZahlungseingaengeKarte() {
  const abfrage = useZahlungseingaengeLesen({ monate: 12 })
  const chf: Monat[] = (abfrage.data ?? [])
    .filter((eingang) => eingang.waehrung === 'CHF')
    .map((eingang) => ({
      monat: eingang.monat,
      bezeichnung: monatKurz(eingang.monat),
      betrag: eingang.betrag,
      automatisch: eingang.automatisch,
      uebrige: Math.max(0, Math.round((eingang.betrag - eingang.automatisch) * 100) / 100),
      anzahl: eingang.anzahl,
    }))
  const marken = achsenmarken(Math.max(0, ...chf.map((monat) => monat.betrag)))

  return (
    <Karte
      id="zahlungseingaenge"
      titel="Zahlungseingänge"
      beschreibung="Gutschriften in CHF je Monat nach Valuta, die letzten zwölf Monate"
    >
      {abfrage.isPending ? (
        <Ladeanzeige className="px-4" />
      ) : abfrage.isError ? (
        <Meldung className="m-4">{fehlertext(abfrage.error)}</Meldung>
      ) : (
        <>
          <ul
            aria-label="Legende"
            className="flex flex-wrap gap-x-4 gap-y-1 px-4 pb-1 text-xs text-slate-600"
          >
            <li className="flex items-center gap-1.5">
              <span
                aria-hidden
                className="size-2.5 rounded-sm"
                style={{ backgroundColor: AKZENT }}
              />
              Automatisch zugeordnet
            </li>
            <li className="flex items-center gap-1.5">
              <span
                aria-hidden
                className="size-2.5 rounded-sm"
                style={{ backgroundColor: ZURUECKTRETEND }}
              />
              Von Hand oder in Klärung
            </li>
          </ul>
          <div className="px-2 pb-2">
            <ResponsiveContainer width="100%" height={HOEHE}>
              <BarChart
                data={chf}
                title="Zahlungseingänge in CHF je Monat, davon automatisch zugeordnet"
                margin={{ top: 8, right: 12, bottom: 0, left: 8 }}
              >
                <CartesianGrid vertical={false} stroke={GITTER} />
                <XAxis
                  dataKey="bezeichnung"
                  tickLine={false}
                  axisLine={{ stroke: ACHSE }}
                  tick={achsenschrift}
                  interval="preserveStartEnd"
                />
                <YAxis
                  tickFormatter={achsenwert}
                  ticks={marken}
                  domain={[0, marken[marken.length - 1] ?? 0]}
                  tickLine={false}
                  axisLine={false}
                  tick={achsenschrift}
                  width={72}
                />
                <Tooltip
                  cursor={{ fill: HOVER }}
                  content={({ active, payload }) => {
                    const monat = payload[0]?.payload as Monat | undefined
                    return active && monat ? <Monatshinweis monat={monat} /> : null
                  }}
                />
                <Bar
                  dataKey="automatisch"
                  name="Automatisch zugeordnet"
                  stackId="eingang"
                  fill={AKZENT}
                  maxBarSize={24}
                  isAnimationActive={false}
                />
                {/* Die weisse Kontur legt 2 px Fläche zwischen die beiden Teile einer Säule. */}
                <Bar
                  dataKey="uebrige"
                  name="Von Hand oder in Klärung"
                  stackId="eingang"
                  fill={ZURUECKTRETEND}
                  stroke={FLAECHE}
                  strokeWidth={2}
                  maxBarSize={24}
                  radius={[4, 4, 0, 0]}
                  isAnimationActive={false}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <Tabellenansicht>
            <Eingangstabelle eingaenge={abfrage.data} />
          </Tabellenansicht>
        </>
      )}
    </Karte>
  )
}

function Monatshinweis({ monat }: { monat: Monat }) {
  const anteil = monat.betrag > 0 ? Math.round((monat.automatisch / monat.betrag) * 100) : 0
  return (
    <Hinweis titel={monat.bezeichnung}>
      <strong className="block text-slate-900">{formatBetrag(monat.betrag, 'CHF')}</strong>
      <span className="flex items-center gap-1.5 text-slate-600">
        <span aria-hidden className="h-0.5 w-3" style={{ backgroundColor: AKZENT }} />
        {formatBetrag(monat.automatisch, 'CHF')} automatisch ({anteil} %)
      </span>
      <span className="flex items-center gap-1.5 text-slate-600">
        <span aria-hidden className="h-0.5 w-3" style={{ backgroundColor: ZURUECKTRETEND }} />
        {formatBetrag(monat.uebrige, 'CHF')} übrige
      </span>
    </Hinweis>
  )
}

function Eingangstabelle({ eingaenge }: { eingaenge: MonatseingangDto[] }) {
  return (
    <Tabelle>
      <thead>
        <tr>
          <Kopfzelle>Monat</Kopfzelle>
          <Kopfzelle>Währung</Kopfzelle>
          <Kopfzelle className="text-right">Eingänge</Kopfzelle>
          <Kopfzelle className="text-right">Automatisch</Kopfzelle>
          <Kopfzelle className="text-right">Gutschriften</Kopfzelle>
        </tr>
      </thead>
      <tbody>
        {eingaenge.map((eingang) => (
          <tr key={`${eingang.monat}-${eingang.waehrung}`}>
            <Zelle>{monatKurz(eingang.monat)}</Zelle>
            <Zelle>{eingang.waehrung}</Zelle>
            <Zelle className="text-right">
              <Betrag betrag={eingang.betrag} waehrung={eingang.waehrung} />
            </Zelle>
            <Zelle className="text-right">
              <Betrag betrag={eingang.automatisch} waehrung={eingang.waehrung} />
            </Zelle>
            <Zelle className="text-right tabular-nums">{eingang.anzahl}</Zelle>
          </tr>
        ))}
      </tbody>
    </Tabelle>
  )
}

function Karte({
  id,
  titel,
  beschreibung,
  children,
}: {
  id: string
  titel: string
  beschreibung: string
  children: ReactNode
}) {
  return (
    <section aria-labelledby={id} className="rounded-lg border border-slate-200 bg-white">
      <header className="px-4 pt-4 pb-2">
        <h2 id={id} className="text-sm font-semibold text-slate-900">
          {titel}
        </h2>
        <p className="text-xs text-slate-500">{beschreibung}</p>
      </header>
      {children}
    </section>
  )
}

/** Die Werte des Diagramms als Tabelle, für alle, die Farben und Formen nicht lesen können oder wollen. */
function Tabellenansicht({ children }: { children: ReactNode }) {
  return (
    <details className="border-t border-slate-100">
      <summary className="cursor-pointer px-4 py-2 text-sm text-slate-600 hover:text-slate-900 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none">
        Werte als Tabelle
      </summary>
      {children}
    </details>
  )
}

function Hinweis({ titel, children }: { titel: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-sm">
      <span className="mb-0.5 block text-slate-500">{titel}</span>
      {children}
    </div>
  )
}
