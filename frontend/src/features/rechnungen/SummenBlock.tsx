import type { ReactNode } from 'react'
import type { SummenDto } from '@/shared/api/generiert/modell'
import { cn } from '@/shared/lib/cn'
import { formatBetrag, formatProzent } from '@/shared/lib/format'
import { Betrag } from '@/shared/ui/Betrag'
import { MWST_BEZEICHNUNG } from './mwst'

/** Netto, MWST je Satz, Rundung und Total, so wie das Backend sie berechnet hat. */
export function SummenBlock({
  summen,
  waehrung,
  className,
}: {
  summen: SummenDto
  waehrung: string
  className?: string
}) {
  return (
    <dl className={cn('space-y-1.5 text-sm', className)}>
      <Summenzeile titel="Netto" betrag={summen.netto} waehrung={waehrung} />
      {summen.mwst.map((anteil) => (
        <Summenzeile
          key={anteil.code}
          titel={
            anteil.code === 'OHNE' ? (
              MWST_BEZEICHNUNG.OHNE
            ) : (
              <>
                MWST {formatProzent(anteil.prozent)}
                <span className="text-slate-500"> auf {formatBetrag(anteil.basis, waehrung)}</span>
              </>
            )
          }
          betrag={anteil.betrag}
          waehrung={waehrung}
        />
      ))}
      {summen.rundung !== 0 && (
        <Summenzeile titel="Rundung auf 5 Rappen" betrag={summen.rundung} waehrung={waehrung} />
      )}
      <Summenzeile
        titel="Total"
        betrag={summen.total}
        waehrung={waehrung}
        className="border-t border-slate-200 pt-1.5 font-semibold text-slate-900"
      />
    </dl>
  )
}

function Summenzeile({
  titel,
  betrag,
  waehrung,
  className,
}: {
  titel: ReactNode
  betrag: number
  waehrung: string
  className?: string
}) {
  return (
    <div className={cn('flex items-baseline justify-between gap-4 text-slate-600', className)}>
      <dt>{titel}</dt>
      <dd>
        <Betrag betrag={betrag} waehrung={waehrung} />
      </dd>
    </div>
  )
}
