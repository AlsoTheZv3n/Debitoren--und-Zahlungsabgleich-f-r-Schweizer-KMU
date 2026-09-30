# Debitoren- und Zahlungsabgleich für Schweizer KMU

QR-Rechnungen erstellen, Zahlungseingänge aus camt.054 automatisch zuordnen und ein mehrstufiges Mahnwesen führen, mandantenfähig von Beginn an. Ziele, Architektur und Roadmap stehen in der [Projektplanung](debitoren-zahlungsabgleich-projektplanung.md).

**Stand:** Phase 4 (Mahnwesen und Übersicht). Staging und Produktion auf Kubernetes folgen in Phase 5.

## Aufbau

```text
backend/             Spring Boot 4.1, Java 25, Spring Modulith, Maven Wrapper
frontend/            React 19, TypeScript, Vite, Tailwind CSS v4, TanStack Query, Orval
e2e/                 Playwright-Tests gegen den Compose-Stack
deploy/compose/      PostgreSQL 18, Keycloak, Mailpit; mit Profil "app" auch Backend und Frontend
.github/workflows/   CI (backend, frontend, api-vertrag, sicherheit, e2e) und release-please
```

## Voraussetzungen

| Werkzeug | Version | Hinweis |
| --- | --- | --- |
| JDK | 25 | `JAVA_HOME` für das Backend darauf setzen; Maven kommt über `./mvnw` |
| Node.js | 24 | siehe `.nvmrc`; pnpm kommt über `packageManager` in `package.json` |
| Docker | aktuell | für Compose und für Testcontainers in den Integrationstests |

## Lokal starten

```bash
# 1. PostgreSQL, Keycloak und Mailpit
docker compose -f deploy/compose/docker-compose.yml up -d --wait

# 2. Backend im Profil dev (Flyway; der Seeder legt zwei Demo-Mandanten mit je 200 Kunden und 1'500 Rechnungen an,
#    spielt Zahlungen als camt.054-Dateien durch den echten Import und mahnt die überfälligen Rechnungen an zwei
#    früheren Stichtagen; die Mahnungen landen in Mailpit, die Vorschläge von heute warten auf Freigabe)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend
cd frontend && pnpm install && pnpm dev
```

Danach <http://localhost:5180> öffnen und anmelden.

| Benutzer | Passwort | Rollen | Mandanten |
| --- | --- | --- | --- |
| `admin` | `admin` | Admin, Buchhaltung, Leser | Berner Werkstatt AG, Seeland Gartenbau GmbH |
| `buchhaltung` | `buchhaltung` | Buchhaltung, Leser | beide |
| `leser` | `leser` | Leser | Berner Werkstatt AG |

Die Zugangsdaten gelten nur lokal. Die Keycloak-Verwaltung liegt unter <http://localhost:8081> (`admin`/`admin`), Mailpit unter <http://localhost:8025>.

**Ohne Backend:** `pnpm dev:mock` startet das Frontend mit MSW-Antworten und einem festen Demo-Admin.

### Ports

| Dienst | Port |
| --- | --- |
| Frontend (Vite) | 5180 |
| Backend (Profil dev) | 8082, Actuator auf 8091 |
| Keycloak | 8081 |
| PostgreSQL | 15432 |
| Mailpit | 8025, SMTP 1025 |
| Frontend-Container (Compose-Profil `app`) | 8088 |

Die Ports weichen bewusst von 5173, 5432 und 8080 ab, die auf Entwicklerrechnern oft schon belegt sind.

## Befehle

| Wo | Befehl | Zweck |
| --- | --- | --- |
| `backend/` | `./mvnw verify` | Formatprüfung, Unit-, Slice-, Integrations- und Architekturtests, Abdeckungsgrenze |
| `backend/` | `./mvnw spotless:apply` | Code formatieren (palantir-java-format) |
| `backend/` | `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev` | Backend mit PostgreSQL aus Testcontainers statt aus Compose (Keycloak muss laufen) |
| `frontend/` | `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build` | wie in der CI |
| `frontend/` | `pnpm api:sync` | `openapi.json` aus dem Backend erzeugen und den Client neu generieren |
| `e2e/` | `pnpm test` | Playwright gegen <http://localhost:8088>; mit `E2E_BASE_URL` gegen ein anderes Ziel |

### E2E wie in der CI

```bash
cd backend && ./mvnw -DskipTests spring-boot:build-image -Dspring-boot.build-image.imageName=debitoren-backend:local
docker build -t debitoren-frontend:local frontend
docker compose -f deploy/compose/docker-compose.yml --profile app up -d --wait
cd e2e && pnpm install && pnpm exec playwright install chromium && pnpm test
```

## Wichtige Regeln

- **API-Vertrag:** `frontend/openapi.json` und `frontend/src/shared/api/generiert` entstehen nur über `pnpm api:sync`, nie von Hand. Die CI bricht ab, wenn sie veraltet sind oder die Änderung die API bricht.
- **Mandanten:** Jede fachliche Tabelle trägt `mandant_id`; Hibernate filtert über `@TenantId`. Der Mandant kommt aus dem Claim `mandant_id` im Token, bei mehreren wählt der Header `X-Mandant-Id`.
- **Rollen:** Das Backend prüft jede Aktion per `@PreAuthorize` auf den Use Cases; die Oberfläche blendet nur aus.
- **Audit-Log:** Hibernate Envers schreibt jede Änderung an Rechnungen, Zuordnungen, Mahnungen, Mahnstufen, Kunden, Mandant und MWST-Sätzen mit Benutzer und Mandant in die `_aud`-Tabellen. Eine neue auditierte Entität braucht ihre `_aud`-Tabelle in einer Migration.
- **Migrationen:** `V001__stammdaten.sql` usw.; eine ausgeführte Migration wird nie geändert. Testdaten kommen aus dem Seeder im Profil `dev`, nicht aus Migrationen.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `chore:` …); release-please erzeugt daraus Version und Changelog.

## Kunden und Rechnungen

- **Kunden** mit Adresse und Zahlungsfrist; bleibt die Kundennummer leer, vergibt das System die nächste freie.
- **Rechnungen** entstehen als Entwurf mit bis zu 100 Positionen. Summen, MWST je Satz und die Rundung auf 5 Rappen (je Mandant abschaltbar) rechnet nur das Backend; der Editor zeigt sie samt QR-Zahlteil während der Eingabe an.
- **MWST-Sätze** sind datiert (Normalsatz, reduzierter Satz, Beherbergung, ohne MWST); es gilt der Satz am Rechnungsdatum.
- **Freigabe** vergibt Nummer (`RE-2026-0001`, lückenlos je Mandant und Jahr) und Referenz: QR-Referenz bei einer QR-IBAN, sonst SCOR-Referenz (ISO 11649). Danach ist die Rechnung unveränderlich und lässt sich nur noch stornieren, solange keine Zahlung eingegangen ist. Die Liste gibt mehrere Entwürfe gesammelt frei.
- **PDF** mit QR-Zahlteil entsteht nach der Freigabe und wird einmalig abgelegt (`DEBITOREN_DOKUMENTE_VERZEICHNIS`, Vorgabe `data/dokumente`); ein abgelegtes Dokument wird nie überschrieben.

## Zahlungsimport und Abgleich

- **Format:** camt.054.001.08 nach ISO 20022, wie ihn die Swiss Payment Standards seit 2022 verwenden. Das XSD liegt unverändert (von iso20022.org) unter `backend/src/main/resources/camt/`; daraus erzeugt das `jaxb-maven-plugin` die Java-Klassen, und jede hochgeladene Datei wird dagegen geprüft. DTD und externe Entitäten sind abgeschaltet (Schutz vor XXE), Uploads sind auf 20 MB begrenzt.
- **Duplikate:** Eine Datei mit bekanntem SHA-256 lehnt der Import mit 409 `DATEI_BEREITS_IMPORTIERT` ab; eine Buchung mit bekannter Bankreferenz (`AcctSvcrRef`) wird übersprungen. Übernommen werden nur gebuchte Gutschriften, jede Transaktion einer Sammelbuchung einzeln; Belastungen und Vormerkungen zählen als ignoriert. Die Datei muss zu einem Konto des Mandanten gehören (IBAN oder QR-IBAN). Das Original liegt unveränderlich in der Dokumentablage.
- **Abgleich:** Nach dem Commit des Imports gleicht das Modul Abgleich jede neue Zahlung ab. Die Strategien laufen in fester Reihenfolge: `ReferenzExaktStrategie` ordnet über die QR- oder SCOR-Referenz automatisch zu; `BetragUndKundeStrategie` (offener Betrag und Name des Auftraggebers passen) macht nur einen Vorschlag. Gleicher Betrag heisst bezahlt, kleinerer teilbezahlt; ein grösserer bezahlt die Rechnung, der Rest bleibt in der Klärung.
- **Klärungsliste:** alles, was sich nicht automatisch zuordnen liess, mit Grund (ohne Referenz, Referenz unbekannt, Überzahlung, andere Währung, stornierte oder bereits bezahlte Rechnung). Links die Zahlungen, rechts Vorschläge und eine Suche nach offenen Rechnungen; zugeordnet wird mit einem Klick, auf Wunsch mit Teilbetrag. Eine Zuordnung lässt sich in der Rechnung unter «Zahlungseingänge» wieder aufheben.
- **Gleichzeitigkeit:** Eine Zuordnung ändert Zahlung, Zuordnung und Rechnung in einer Transaktion und erhöht die Versionen von Zahlung und Rechnung; wer eine veraltete Zahlung zuordnet, erhält 409.
- **Demo-Datei:** Im Profil `dev` erzeugt `POST /api/dev/camt-testdatei` eine camt.054-Datei aus den offenen Rechnungen des gewählten Mandanten, mit jedem Fall des Abgleichs. Am einfachsten über die Swagger-Oberfläche unter <http://localhost:8082/swagger-ui.html>: unter «Authorize» das Token eintragen (im Browser nach der Anmeldung unter Session Storage, Eintrag `oidc.user:…`, Feld `access_token`), bei Benutzern mit mehreren Mandanten dazu die Mandanten-ID; die heruntergeladene Datei dann im Import hochladen.

## Mahnwesen

- **Mahnstufen** je Mandant, in den Einstellungen änderbar (bis fünf): Tage nach Fälligkeit, Mindestabstand zur vorherigen Stufe, Zahlungsfrist, Gebühr und Brieftext mit den Platzhaltern `{rechnungsnummer}`, `{faelligAm}`, `{offen}`, `{frist}` und `{gebuehr}`. Jeder neue Mandant erhält die Stufen der Projektplanung: Zahlungserinnerung nach 10 Tagen, 1. Mahnung nach 30 Tagen, 2. Mahnung nach 45 Tagen mit CHF 20.00 Gebühr.
- **Mahnlauf:** täglich um 6 Uhr (`DEBITOREN_MAHNLAUF_ZEITPLAN`, Cron; `-` schaltet ihn ab), mit ShedLock bei mehreren Instanzen nur einmal; Admins starten ihn auch von Hand. Gemahnt wird Stufe um Stufe, nie übersprungen. Vorschläge zu inzwischen bezahlten oder stornierten Rechnungen verwirft der Lauf selbst.
- **Freigabe:** gesammelt, jede Mahnung in eigener Transaktion. Erst jetzt belastet die Gebühr die Rechnung; der offene Posten ist ab dann Total plus Gebühren minus Zahlungen, bezahlt ist die Rechnung erst mit den Gebühren. Das Schreiben hält Beträge und Text der Freigabe fest.
- **Versand:** nach dem Commit als PDF mit QR-Zahlteil (offener Betrag, Referenz der Rechnung) per E-Mail. Absender ist `DEBITOREN_MAIL_ABSENDER` mit dem Namen des Mandanten, Antworten gehen an dessen E-Mail-Adresse. Ohne E-Mail-Adresse des Kunden liegt das Schreiben unter «Zum Versand» zum Drucken; nach dem Versand per Post bestätigt die Buchhaltung ihn dort. Scheitert der Mailserver, bleibt die Mahnung mit dem Fehler stehen und lässt sich erneut senden.
- **Verwerfen:** mit Grund und auf Wunsch einer Sperre bis zu einem Datum; bis dahin schlägt der Mahnlauf die Rechnung nicht vor.
- **Mail lokal:** Mailpit aus Compose (<http://localhost:8025>); Umgebungsvariablen `MAIL_HOST` und `MAIL_PORT`, produktiv zusätzlich `MAIL_USERNAME` und `MAIL_PASSWORD` mit STARTTLS.

## Übersicht, Audit-Log und Einstellungen

- **Dashboard:** offene und überfällige Posten, Klärungsliste, Mahnvorschläge und der Anteil automatisch zugeordneter Zahlungen der letzten 30 Tage; dazu die Altersstruktur (nicht fällig, 1–30, 31–60, 61–90, über 90 Tage) und die Zahlungseingänge der letzten zwölf Monate. Beide Diagramme haben eine Tabelle mit denselben Werten.
- **Verlauf:** Jede Rechnung zeigt ihre Mahnungen und den Verlauf aus dem Audit-Log, samt Zahlungseingängen. Das ganze Log mit Filter nach Objekt und Benutzer steht unter Einstellungen (nur Admin).
- **Einstellungen** (nur Admin): Mandant mit Adresse, IBAN und QR-IBAN (neue Rechnungen folgen dem Konto, freigegebene bleiben), Mahnstufen und neue MWST-Sätze ab Stichtag; der bisherige Satz desselben Codes endet am Vortag.

## Gate 1

| Kriterium | Nachweis |
| --- | --- |
| Pipeline grün | alle Jobs aus `.github/workflows/ci.yml` lokal grün; auf GitHub erst nach dem ersten Push |
| Login mit Rollen klappt | `e2e/tests/anmeldung.spec.ts` gegen Keycloak, Frontend-Container, Backend und PostgreSQL |

## Gate 2

| Kriterium | Nachweis |
| --- | --- |
| QR-Zahlteil im SIX-Portal fehlerfrei validiert | manuell, siehe unten |
| Rechnungsablauf durch alle Schichten | `e2e/tests/rechnungen.spec.ts`: Kunde anlegen, Rechnung erfassen, freigeben, PDF laden, stornieren, Sammelfreigabe |

**QR-Zahlteil prüfen:** `./mvnw verify` im Ordner `backend` legt drei Beispiel-PDFs unter `backend/target/qr-zahlteil-beispiele/` ab: `qr-referenz-chf.pdf` (QR-IBAN mit QR-Referenz), `scor-referenz-chf.pdf` (IBAN mit SCOR-Referenz) und `qr-referenz-eur.pdf`. Alternativ eine freigegebene Rechnung in der Anwendung über «PDF herunterladen» speichern. Die PDFs im Validierungsportal von SIX (<https://validation.iso-payments.ch>, Registrierung nötig) hochladen; alle drei müssen ohne Fehler durchgehen.

## Gate 3

| Kriterium | Nachweis |
| --- | --- |
| Alle camt-Testszenarien richtig zugeordnet | `CamtSzenarienIT` im Backend: 13 Golden Files unter `backend/src/test/resources/camt/`, je eine camt.054-Datei und daneben das erwartete Ergebnis als JSON |
| Hauptfluss durch alle Schichten | `e2e/tests/zahlungen.spec.ts`: Rechnung freigeben, camt-Datei importieren, Rechnung bezahlt; Zahlung ohne Referenz in der Klärungsliste zuordnen; doppelte Datei abgelehnt |

Die Szenarien decken die Liste der Projektplanung ab: exakte Zahlung, Teil- und Überzahlung, zwei Zahlungen für eine Rechnung, dieselbe Datei nochmals, bekannte Buchung in neuer Datei, unbekannte Referenz, ohne Referenz mit passendem Betrag und Namen, Sammelbuchung (mit Belastung und Vormerkung), EUR-Zahlung auf CHF-Rechnung, stornierte Rechnung; dazu SCOR-Referenz und eine doppelt bezahlte Rechnung. Einzeln laufen lassen:

```bash
cd backend && ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=CamtSzenarienIT -Djacoco.skip=true
```

Ein neues Szenario ist eine neue JSON-Datei mit passender XML-Datei; der Test findet es selbst.

## Gate 4

| Kriterium | Nachweis |
| --- | --- |
| E2E-Hauptfluss in Playwright grün | `e2e/tests/mahnwesen.spec.ts`: überfällige Rechnung, Mahnlauf, Freigabe, Versand per E-Mail bis Mailpit samt PDF-Anhang, Mahnung und Verlauf in der Rechnung; dazu WCAG 2.2 AA für Dashboard, Mahnwesen und Einstellungen und die Rollen |

Im Backend prüfen `MahnwesenIT`, `ReportingIT`, `EinstellungenIT` und `AuditErfassungIT` dieselben Abläufe gegen PostgreSQL, der Versand gegen einen SMTP-Server im Test (GreenMail). `./mvnw verify` legt zusätzlich ein Mahnschreiben unter `backend/target/qr-zahlteil-beispiele/mahnung-qr-referenz-chf.pdf` ab; es lässt sich wie die Rechnungen im SIX-Portal prüfen.
