import { AlertTriangle, ArrowRight, BellRing, FileText, Landmark, Zap } from 'lucide-react'
import { lazy, Suspense, type ReactNode } from 'react'
import { Link } from 'react-router'
import { useAktuellerBenutzer, useUebersichtLesen } from '@/shared/api/generiert/endpunkte'
import type { SummeDto, UebersichtDto } from '@/shared/api/generiert/modell'
import { cn } from '@/shared/lib/cn'
import { formatBetrag, formatZeitpunkt } from '@/shared/lib/format'
import { fehlertext } from '@/shared/lib/http'
import { useMandant } from '@/shared/lib/mandant'
import { ROLLEN, useRollen } from '@/shared/lib/rollen'
import { Badge } from '@/shared/ui/Badge'
import { Ladeanzeige } from '@/shared/ui/Ladeanzeige'
import { Meldung } from '@/shared/ui/Meldung'
import { Seitenkopf } from '@/shared/ui/Seitenkopf'

// Recharts ist gross; die Diagramme laden nach, die Kennzahlen stehen sofort.
const AltersstrukturKarte = lazy(async () => ({
  default: (await import('./Diagramme')).AltersstrukturKarte,
}))
const ZahlungseingaengeKarte = lazy(async () => ({
  default: (await import('./Diagramme')).ZahlungseingaengeKarte,
}))

export function DashboardSeite() {
  const { data: benutzer } = useAktuellerBenutzer()
  const uebersicht = useUebersichtLesen()

  return (
    <>
      <Seitenkopf
        titel="Dashboard"
        beschreibung={benutzer ? `Guten Tag, ${benutzer.anzeigename}` : undefined}
      />

      {uebersicht.isPending ? (
        <Ladeanzeige />
      ) : uebersicht.isError ? (
        <Meldung className="mb-6">{fehlertext(uebersicht.error)}</Meldung>
      ) : (
        <Kennzahlen uebersicht={uebersicht.data} />
      )}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Suspense fallback={<Ladeanzeige />}>
          <AltersstrukturKarte />
        </Suspense>
        <Suspense fallback={<Ladeanzeige />}>
          <ZahlungseingaengeKarte />
        </Suspense>
      </div>

      {uebersicht.data && <Aktivitaet uebersicht={uebersicht.data} />}

      <Sitzung />
    </>
  )
}

function Kennzahlen({ uebersicht }: { uebersicht: UebersichtDto }) {
  const offen = hauptsumme(uebersicht.offen)
  const ueberfaellig = hauptsumme(uebersicht.ueberfaellig)
  const klaerung = uebersicht.inKlaerung.reduce((summe, eintrag) => summe + eintrag.anzahl, 0)
  const anteil =
    uebersicht.zahlungen30Tage > 0
      ? Math.round((uebersicht.automatisch30Tage / uebersicht.zahlungen30Tage) * 100)
      : null

  return (
    <section aria-label="Kennzahlen" className="mb-6">
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Kennzahl
          titel="Offene Posten"
          symbol={FileText}
          wert={formatBetrag(offen.betrag, offen.waehrung)}
          ziel="/rechnungen?offen=ja"
        >
          {anzahlText(offen.anzahl, 'Rechnung', 'Rechnungen')}
          {nebenwaehrungen(uebersicht.offen)}
        </Kennzahl>
        <Kennzahl
          titel="Überfällig"
          symbol={AlertTriangle}
          wert={formatBetrag(ueberfaellig.betrag, ueberfaellig.waehrung)}
          art="ueberfaellig"
          ziel="/rechnungen?ueberfaellig=ja"
        >
          {anzahlText(ueberfaellig.anzahl, 'Rechnung', 'Rechnungen')}
          {nebenwaehrungen(uebersicht.ueberfaellig)}
        </Kennzahl>
        <Kennzahl
          titel="Klärungsliste"
          symbol={Landmark}
          wert={String(klaerung)}
          art={klaerung > 0 ? 'klaerung' : undefined}
          ziel="/zahlungen/klaerung"
        >
          {klaerung === 1 ? 'Zahlung wartet auf Zuordnung' : 'Zahlungen warten auf Zuordnung'}
        </Kennzahl>
        <Kennzahl
          titel="Mahnvorschläge"
          symbol={BellRing}
          wert={String(uebersicht.mahnvorschlaege)}
          ziel="/mahnwesen"
        >
          {uebersicht.letzterMahnlauf
            ? `Letzter Lauf ${formatZeitpunkt(uebersicht.letzterMahnlauf.ausgefuehrtAm)}`
            : 'Noch kein Mahnlauf'}
        </Kennzahl>
        <Kennzahl
          titel="Automatisch zugeordnet"
          symbol={Zap}
          wert={anteil === null ? '–' : `${String(anteil)} %`}
        >
          {uebersicht.zahlungen30Tage > 0
            ? `${String(uebersicht.automatisch30Tage)} von ${String(uebersicht.zahlungen30Tage)} Zahlungen, 30 Tage`
            : 'Keine Zahlungen in den letzten 30 Tagen'}
        </Kennzahl>
      </ul>
    </section>
  )
}

const kennzahlArt = {
  ueberfaellig: 'text-status-ueberfaellig',
  klaerung: 'text-status-klaerung',
}

/**
 * Eine Kennzahl als Kachel. Führt sie zu einer Liste, ist die ganze Kachel der Link; Status zeigt sich nie nur
 * über die Farbe, das Symbol und der Titel tragen ihn mit.
 */
function Kennzahl({
  titel,
  wert,
  symbol: Zeichen,
  art,
  ziel,
  children,
}: {
  titel: string
  wert: string
  symbol: typeof FileText
  art?: keyof typeof kennzahlArt
  ziel?: string
  children: ReactNode
}) {
  const inhalt = (
    <>
      <span className="flex items-center gap-1.5 text-sm text-slate-600">
        <Zeichen aria-hidden className={cn('size-4', art ? kennzahlArt[art] : 'text-slate-400')} />
        {titel}
      </span>
      <span
        className={cn(
          'mt-1 block text-xl font-semibold tracking-tight text-slate-900',
          art && kennzahlArt[art],
        )}
      >
        {wert}
      </span>
      <span className="mt-0.5 block text-xs text-slate-500">{children}</span>
    </>
  )
  const rahmen = 'block h-full rounded-lg border border-slate-200 bg-white px-4 py-3'
  return (
    <li>
      {ziel ? (
        <Link
          to={ziel}
          className={cn(
            rahmen,
            'transition-colors hover:border-slate-300 hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none',
          )}
        >
          {inhalt}
        </Link>
      ) : (
        <div className={rahmen}>{inhalt}</div>
      )}
    </li>
  )
}

/** Franken zuerst; ohne Franken die erste Währung, ohne Posten null Franken. */
function hauptsumme(summen: SummeDto[]): SummeDto {
  return (
    summen.find((summe) => summe.waehrung === 'CHF') ??
    summen[0] ?? { waehrung: 'CHF', betrag: 0, anzahl: 0 }
  )
}

function nebenwaehrungen(summen: SummeDto[]): ReactNode {
  const weitere = summen.filter((summe) => summe.waehrung !== 'CHF')
  if (weitere.length === 0 || !summen.some((summe) => summe.waehrung === 'CHF')) {
    return null
  }
  return weitere.map((summe) => (
    <span key={summe.waehrung} className="block">
      dazu {formatBetrag(summe.betrag, summe.waehrung)}
    </span>
  ))
}

function anzahlText(anzahl: number, einzahl: string, mehrzahl: string): string {
  return anzahl === 1 ? `1 ${einzahl}` : `${anzahl.toLocaleString('de-CH')} ${mehrzahl}`
}

function Aktivitaet({ uebersicht }: { uebersicht: UebersichtDto }) {
  return (
    <section
      aria-labelledby="letzte-importe"
      className="mb-6 rounded-lg border border-slate-200 bg-white"
    >
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h2 id="letzte-importe" className="text-sm font-semibold text-slate-900">
          Letzte Importe
        </h2>
        <Link
          to="/zahlungen/import"
          className="flex items-center gap-1 text-sm text-brand-700 hover:underline focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none"
        >
          Zum Importprotokoll
          <ArrowRight aria-hidden className="size-4" />
        </Link>
      </div>
      {uebersicht.letzteImporte.length === 0 ? (
        <p className="px-4 py-4 text-sm text-slate-500">Noch keine Bankdatei importiert.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {uebersicht.letzteImporte.map((datei) => (
            <li
              key={datei.id}
              className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-2 text-sm"
            >
              <span className="text-slate-500 tabular-nums">
                {formatZeitpunkt(datei.importiertAm)}
              </span>
              <span className="min-w-0 flex-1 truncate text-slate-900">{datei.dateiname}</span>
              <span className="text-slate-600">
                {anzahlText(datei.neu, 'neue Gutschrift', 'neue Gutschriften')}
              </span>
              {datei.inKlaerung > 0 && (
                <Badge variante="klaerung">{datei.inKlaerung} in Klärung</Badge>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function Sitzung() {
  const { data: benutzer } = useAktuellerBenutzer()
  const { aktiv } = useMandant()
  const { rollen, hauptrolle } = useRollen()

  return (
    <section
      aria-labelledby="sitzung-titel"
      className="rounded-lg border border-slate-200 bg-white"
    >
      <h2
        id="sitzung-titel"
        className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900"
      >
        Ihre Sitzung
      </h2>
      <dl className="grid gap-x-8 gap-y-4 px-4 py-4 text-sm sm:grid-cols-3">
        <div>
          <dt className="text-slate-500">Angemeldet als</dt>
          <dd className="mt-1 font-medium text-slate-900">{benutzer?.anzeigename}</dd>
          <dd className="text-slate-500">{benutzer?.benutzername}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Mandant</dt>
          <dd className="mt-1 font-medium text-slate-900">{aktiv.name}</dd>
          <dd className="flex items-center gap-2 text-slate-500">
            {aktiv.ort}
            <Badge>{aktiv.referenzart === 'QR' ? 'QR-Referenz' : 'SCOR-Referenz'}</Badge>
          </dd>
        </div>
        <div>
          <dt className="text-slate-500">Rollen</dt>
          <dd className="mt-1 flex flex-wrap gap-1">
            {rollen.map((rolle) => (
              <Badge key={rolle} variante="brand">
                {ROLLEN[rolle].name}
              </Badge>
            ))}
          </dd>
        </div>
      </dl>
      {hauptrolle && (
        <p className="border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
          <span className="font-medium text-slate-900">Berechtigung: </span>
          {ROLLEN[hauptrolle].darf}
        </p>
      )}
    </section>
  )
}
