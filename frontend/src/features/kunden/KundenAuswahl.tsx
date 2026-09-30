import { keepPreviousData } from '@tanstack/react-query'
import { useState } from 'react'
import { useKundenSuchen } from '@/shared/api/generiert/endpunkte'
import type { KundeDto } from '@/shared/api/generiert/modell'
import { cn } from '@/shared/lib/cn'
import { useEntprellt } from '@/shared/lib/useEntprellt'
import { Eingabe } from '@/shared/ui/Eingabe'
import type { FeldSteuerung } from '@/shared/ui/Feld'

/** Gewählter Kunde; bei einem gespeicherten Entwurf sind nur Id und Name bekannt. */
export interface KundeWahl {
  id: string
  name: string
  kundennummer?: string
  zahlungsfristTage?: number
}

const anzeige = (kunde: KundeWahl) =>
  kunde.kundennummer ? `${kunde.name} (${kunde.kundennummer})` : kunde.name

/**
 * Suchfeld mit Vorschlagsliste nach dem Combobox-Muster der WAI-ARIA: Pfeiltasten wählen, Enter
 * übernimmt, Escape schliesst. Wer tippt, hebt eine frühere Wahl auf.
 */
export function KundenAuswahl({
  wert,
  onWahl,
  disabled,
  ...steuerung
}: FeldSteuerung & {
  wert: KundeWahl | null
  onWahl: (kunde: KundeWahl | null) => void
  disabled?: boolean
}) {
  const [text, setText] = useState(wert ? anzeige(wert) : '')
  const [offen, setOffen] = useState(false)
  const [aktiv, setAktiv] = useState(0)
  const suche = useEntprellt(text, 250)
  const treffer = useKundenSuchen(
    { suche: suche.trim() || undefined, groesse: 8 },
    { query: { enabled: offen, placeholderData: keepPreviousData } },
  )
  const liste = treffer.data?.inhalt ?? []
  // Solange die Treffer noch zu einer früheren Eingabe gehören, übernimmt Enter nichts.
  const aktuell = suche === text && !treffer.isPlaceholderData
  const listeId = `${steuerung.id}-liste`
  const sichtbar = offen && liste.length > 0

  const waehlen = (kunde: KundeDto) => {
    onWahl({
      id: kunde.id,
      name: kunde.name,
      kundennummer: kunde.kundennummer,
      zahlungsfristTage: kunde.zahlungsfristTage,
    })
    setText(anzeige(kunde))
    setOffen(false)
  }

  return (
    <div className="relative">
      <Eingabe
        {...steuerung}
        role="combobox"
        aria-expanded={sichtbar}
        aria-controls={listeId}
        aria-autocomplete="list"
        aria-activedescendant={sichtbar ? `${listeId}-${aktiv}` : undefined}
        autoComplete="off"
        placeholder="Kunde suchen"
        value={text}
        disabled={disabled}
        onChange={(ereignis) => {
          setText(ereignis.target.value)
          setOffen(true)
          setAktiv(0)
          if (wert) {
            onWahl(null)
          }
        }}
        onFocus={() => {
          if (!wert) {
            setOffen(true)
          }
        }}
        onBlur={() => {
          setOffen(false)
        }}
        onKeyDown={(ereignis) => {
          if (ereignis.key === 'ArrowDown') {
            ereignis.preventDefault()
            setOffen(true)
            setAktiv((index) => Math.min(index + 1, Math.max(liste.length - 1, 0)))
          } else if (ereignis.key === 'ArrowUp') {
            ereignis.preventDefault()
            setAktiv((index) => Math.max(index - 1, 0))
          } else if (ereignis.key === 'Enter' && sichtbar) {
            ereignis.preventDefault()
            const kunde = liste[aktiv]
            if (kunde && aktuell) {
              waehlen(kunde)
            }
          } else if (ereignis.key === 'Escape') {
            setOffen(false)
          }
        }}
      />
      <ul
        id={listeId}
        role="listbox"
        aria-label="Vorschläge"
        hidden={!sichtbar}
        className="absolute z-20 mt-1 max-h-64 w-full overflow-auto rounded-lg border border-slate-200 bg-white py-1 text-sm shadow-sm"
      >
        {liste.map((kunde, index) => (
          <li
            key={kunde.id}
            id={`${listeId}-${index}`}
            role="option"
            aria-selected={index === aktiv}
            className={cn(
              'cursor-default px-3 py-1.5',
              index === aktiv && 'bg-brand-50 text-brand-700',
            )}
            onMouseDown={(ereignis) => {
              // Verhindert den Blur des Eingabefelds, bevor der Klick zählt.
              ereignis.preventDefault()
              waehlen(kunde)
            }}
            onMouseEnter={() => {
              setAktiv(index)
            }}
          >
            <span className="font-medium">{kunde.name}</span>
            <span className="ml-2 text-xs text-slate-500">
              {kunde.kundennummer} · {kunde.plz} {kunde.ort}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
