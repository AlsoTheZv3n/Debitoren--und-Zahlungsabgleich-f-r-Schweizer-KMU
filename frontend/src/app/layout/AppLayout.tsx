import { Link, Outlet } from 'react-router'
import { MandantProvider } from '../mandant/MandantProvider'
import { BenutzerMenue } from './BenutzerMenue'
import { Brotkrumen } from './Brotkrumen'
import { MandantWahl } from './MandantWahl'
import { Seitenleiste } from './Seitenleiste'

/** Seitenleiste links, Kopfzeile mit Mandantenwahl und Benutzermenü oben, Inhalt rechts. */
export function AppLayout() {
  return (
    <MandantProvider>
      <a
        href="#inhalt"
        className="sr-only z-30 rounded-lg bg-white px-3 py-2 text-sm font-medium focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:ring-2 focus:ring-brand-600"
      >
        Zum Inhalt springen
      </a>
      <div className="grid min-h-dvh grid-cols-1 grid-rows-[auto_auto_1fr] md:grid-cols-[15rem_minmax(0,1fr)] md:grid-rows-[auto_1fr]">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-4 border-b border-slate-200 bg-white px-3 md:col-span-2 md:px-4">
          <Link
            to="/"
            className="flex shrink-0 items-center gap-2 rounded-lg text-sm font-semibold text-slate-900 focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:outline-none md:w-52"
          >
            <img src="/favicon.svg" alt="" className="size-6" />
            Debitoren
          </Link>
          <Brotkrumen className="hidden sm:block" />
          <div className="ml-auto flex items-center gap-1">
            <MandantWahl />
            <BenutzerMenue />
          </div>
        </header>
        <Seitenleiste />
        <main id="inhalt" tabIndex={-1} className="min-w-0 px-4 py-6 outline-none md:px-8">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </MandantProvider>
  )
}
