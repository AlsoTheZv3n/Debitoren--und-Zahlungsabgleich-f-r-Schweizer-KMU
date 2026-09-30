import { useEffect, useState } from 'react'

/** Liefert den Wert erst, wenn er sich eine Weile nicht mehr geändert hat, etwa bei Suchfeldern. */
export function useEntprellt<T>(wert: T, millisekunden = 300): T {
  const [entprellt, setEntprellt] = useState(wert)

  useEffect(() => {
    const zeitgeber = setTimeout(() => {
      setEntprellt(wert)
    }, millisekunden)
    return () => {
      clearTimeout(zeitgeber)
    }
  }, [wert, millisekunden])

  return entprellt
}
