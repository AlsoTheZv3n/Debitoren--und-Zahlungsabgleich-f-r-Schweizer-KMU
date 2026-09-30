import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { defineConfig } from 'orval'

const ENDPUNKTE = 'src/shared/api/generiert/endpunkte'

/**
 * Orval erzeugt für binäre Antworten (PDF, SVG) Mock-Daten als Text, obwohl der Typ Blob ist. Der Hook
 * verpackt diese Texte nach jeder Generierung in einen Blob.
 */
function binaereMocksReparieren() {
  const dateien = readdirSync(ENDPUNKTE, { recursive: true, encoding: 'utf8' }).filter((datei) =>
    datei.endsWith('.msw.ts'),
  )
  for (const datei of dateien) {
    const pfad = join(ENDPUNKTE, datei)
    const alt = readFileSync(pfad, 'utf8')
    // Vor dem Formatieren steht der Mock in Klammern: `(): Blob => (faker.string.alpha(…))`.
    const neu = alt.replace(
      /(ResponseMock = \(\): Blob =>\s*)\((faker\.[^\n;]+)\)(?=;?\s*\n)/g,
      '$1new Blob([$2])',
    )
    if (neu !== alt) {
      writeFileSync(pfad, neu)
    }
  }
}

/**
 * Generiert aus openapi.json TanStack-Query-Hooks, Typen, MSW-Handler und Zod-Schemas für Formulare.
 * Das Ergebnis unter src/shared/api/generiert nie von Hand ändern, sondern `pnpm api:sync` ausführen.
 */
export default defineConfig({
  debitoren: {
    input: { target: './openapi.json' },
    output: {
      mode: 'tags-split',
      target: ENDPUNKTE,
      schemas: 'src/shared/api/generiert/modell',
      client: 'react-query',
      httpClient: 'fetch',
      clean: true,
      formatter: 'prettier',
      mock: {
        generators: [{ type: 'msw', locale: 'de_CH' }],
      },
      override: {
        mutator: { path: 'src/shared/lib/http.ts', name: 'apiFetch' },
        fetch: { includeHttpResponseReturnType: false },
      },
    },
    hooks: { afterAllFilesWrite: binaereMocksReparieren },
  },
  // Dieselben Regeln wie die Bean Validation im Backend, für die Validierung der Formulare.
  debitorenZod: {
    input: { target: './openapi.json' },
    output: {
      mode: 'tags-split',
      target: 'src/shared/api/generiert/zod',
      client: 'zod',
      fileExtension: '.zod.ts',
      clean: true,
      formatter: 'prettier',
      override: {
        zod: {
          generate: { param: false, query: false, header: false, body: true, response: false },
        },
      },
    },
  },
})
