// Holt die aktuelle API-Beschreibung aus dem Backend und generiert daraus den Client neu:
// 1. Backend startet mit PostgreSQL aus Testcontainers und schreibt target/openapi.json (Maven-Profil openapi).
// 2. Die Datei wird nach frontend/openapi.json kopiert; sie ist der versionierte API-Vertrag.
// 3. Orval erzeugt Hooks, Typen und MSW-Handler unter src/shared/api/generiert.
import { execSync } from 'node:child_process'
import { copyFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontend = join(dirname(fileURLToPath(import.meta.url)), '..')
const backend = join(frontend, '..', 'backend')
// Feste Befehle ohne Eingaben von aussen; .cmd-Dateien startet Node unter Windows nur über eine Shell.
// Expliziter Pfad: cmd.exe sucht je nach Einstellung nicht im aktuellen Verzeichnis.
const mvnw = process.platform === 'win32' ? '.\\mvnw.cmd' : './mvnw'

execSync(`${mvnw} -B -q -Popenapi -DskipTests verify`, { cwd: backend, stdio: 'inherit' })
copyFileSync(join(backend, 'target', 'openapi.json'), join(frontend, 'openapi.json'))
execSync('pnpm api:generate', { cwd: frontend, stdio: 'inherit' })
