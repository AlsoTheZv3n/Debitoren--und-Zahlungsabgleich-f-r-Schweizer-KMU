import { FileUp } from 'lucide-react'
import { useId, useRef, useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/Button'

/**
 * Fläche für Bankdateien: hineinziehen oder über den Knopf wählen. Der Knopf macht die Fläche auch per
 * Tastatur bedienbar; das eigentliche Dateifeld bleibt versteckt.
 */
export function Dateiablage({
  onDateien,
  beschaeftigt,
}: {
  onDateien: (dateien: File[]) => void
  beschaeftigt: boolean
}) {
  const feld = useRef<HTMLInputElement>(null)
  const hinweisId = useId()
  const [ueber, setUeber] = useState(false)

  const uebernehmen = (liste: FileList | null) => {
    const dateien = Array.from(liste ?? [])
    if (dateien.length > 0) {
      onDateien(dateien)
    }
  }

  return (
    <div
      onDragOver={(ereignis) => {
        ereignis.preventDefault()
        setUeber(true)
      }}
      onDragLeave={() => {
        setUeber(false)
      }}
      onDrop={(ereignis) => {
        ereignis.preventDefault()
        setUeber(false)
        if (!beschaeftigt) {
          uebernehmen(ereignis.dataTransfer.files)
        }
      }}
      className={cn(
        'flex flex-col items-center gap-3 rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors',
        ueber ? 'border-brand-600 bg-brand-50' : 'border-slate-300 bg-white',
      )}
    >
      <FileUp aria-hidden className="size-8 text-slate-500" strokeWidth={1.5} />
      <p id={hinweisId} className="text-sm text-slate-600">
        camt.054-Dateien der Bank hierher ziehen oder auswählen. Bekannte Dateien und Buchungen
        erkennt der Import selbst.
      </p>
      <Button
        variante="sekundaer"
        disabled={beschaeftigt}
        aria-describedby={hinweisId}
        onClick={() => feld.current?.click()}
      >
        {beschaeftigt ? 'Wird importiert…' : 'Dateien wählen'}
      </Button>
      <input
        ref={feld}
        type="file"
        accept=".xml,application/xml,text/xml"
        multiple
        hidden
        data-testid="dateifeld"
        onChange={(ereignis) => {
          uebernehmen(ereignis.target.files)
          ereignis.target.value = ''
        }}
      />
    </div>
  )
}
