package ch.kmu.debitoren.rechnung;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.ApiTestBasis;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Lebenslauf einer Rechnung über die API: Entwurf, Änderung, Freigabe, PDF, Liste, Storno. */
class RechnungIT extends ApiTestBasis {

    private UUID mandant;
    private String buchhaltung;
    private UUID kunde;

    @BeforeEach
    void vorbereiten() {
        mandant = mandantMitQrIban("Berner Werkstatt AG");
        buchhaltung = token("buchhaltung", mandant);
        kunde = kundeAnlegen(buchhaltung, mandant, "Keller Haustechnik AG");
    }

    @Test
    void entwurfFreigebenUndStornieren() {
        var entwurf = anlegen(entwurf(LocalDate.of(2026, 9, 1)));
        assertThat(entwurf.get("status")).isEqualTo("ENTWURF");
        assertThat(entwurf.get("rechnungsnummer")).isNull();
        assertThat(entwurf.get("faelligAm")).isEqualTo("2026-10-01");
        assertThat(summe(entwurf, "total")).isEqualTo(464.55);
        assertThat(summe(entwurf, "rundung")).isEqualTo(-0.02);
        var id = entwurf.get("id");

        // Eine veraltete Version wird abgelehnt, die aktuelle angenommen.
        client.put()
                .uri("/api/rechnungen/" + id)
                .headers(als(buchhaltung, mandant))
                .body(Map.of("version", 7, "entwurf", entwurf(LocalDate.of(2026, 9, 1))))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("GLEICHZEITIG_GEAENDERT");
        var geaendert = new java.util.HashMap<>(entwurf(LocalDate.of(2026, 9, 2)));
        geaendert.put("bemerkung", "Arbeiten gemäss Offerte");
        client.put()
                .uri("/api/rechnungen/" + id)
                .headers(als(buchhaltung, mandant))
                .body(Map.of("version", 0, "entwurf", geaendert))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.bemerkung")
                .isEqualTo("Arbeiten gemäss Offerte")
                .jsonPath("$.version")
                .isEqualTo(1);

        var offen = post("/api/rechnungen/" + id + "/freigabe", Map.of("version", 1));
        assertThat(offen.get("status")).isEqualTo("OFFEN");
        assertThat(offen.get("rechnungsnummer")).isEqualTo("RE-2026-0001");
        assertThat(offen.get("referenzart")).isEqualTo("QR");
        assertThat((String) offen.get("referenz")).startsWith("00 00000").hasSize(32);

        client.put()
                .uri("/api/rechnungen/" + id)
                .headers(als(buchhaltung, mandant))
                .body(Map.of("version", offen.get("version"), "entwurf", entwurf(LocalDate.of(2026, 9, 2))))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("NUR_ENTWURF_AENDERBAR");

        var pdf = client.get()
                .uri("/api/rechnungen/" + id + "/pdf")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PDF)
                .expectHeader()
                .valueMatches(HttpHeaders.CONTENT_DISPOSITION, ".*RE-2026-0001\\.pdf.*")
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        client.get()
                .uri("/api/rechnungen/" + id + "/zahlteil.svg")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(svg -> assertThat(svg).contains("<svg").contains("464.55"));

        client.get()
                .uri("/api/rechnungen?status=OFFEN&suche=re-2026-0001")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(1)
                .jsonPath("$.inhalt[0].offen")
                .isEqualTo(464.55)
                .jsonPath("$.inhalt[0].kundeName")
                .isEqualTo("Keller Haustechnik AG");

        var storniert =
                post("/api/rechnungen/" + id + "/storno", Map.of("version", offen.get("version"), "grund", "Doppelt"));
        assertThat(storniert.get("status")).isEqualTo("STORNIERT");
        assertThat(storniert.get("stornoGrund")).isEqualTo("Doppelt");
        client.get()
                .uri("/api/rechnungen?offen=true")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(0);
    }

    @Test
    void entwurfLoeschenUndNichtGefunden() {
        var id = anlegen(entwurf(LocalDate.of(2026, 9, 1))).get("id");

        client.get()
                .uri("/api/rechnungen/" + id + "/pdf")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("PDF_NUR_NACH_FREIGABE");
        client.delete()
                .uri("/api/rechnungen/" + id + "?version=0")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isNoContent();
        client.get()
                .uri("/api/rechnungen/" + id)
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("RECHNUNG_NICHT_GEFUNDEN");
    }

    @Test
    void nummernLaufenJeJahrUndScorOhneQrIban() {
        var andere = mandantMitIban("Seeland Gartenbau GmbH");
        var token = token("buchhaltung", andere);
        var kundeB = kundeAnlegen(token, andere, "Gasser Treuhand");

        var alt = client.post()
                .uri("/api/rechnungen")
                .headers(als(token, andere))
                .body(entwurf(kundeB, LocalDate.of(2025, 12, 31)))
                .exchange()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        client.post()
                .uri("/api/rechnungen/" + alt.get("id") + "/freigabe")
                .headers(als(token, andere))
                .body(Map.of("version", 0))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.rechnungsnummer")
                .isEqualTo("RE-2025-0001")
                .jsonPath("$.referenzart")
                .isEqualTo("SCOR")
                .jsonPath("$.referenz")
                .value(referenz -> assertThat((String) referenz).startsWith("RF"));
    }

    @Test
    void ueberfaelligeRechnungenFiltern() {
        var entwurf = new java.util.HashMap<>(entwurf(LocalDate.of(2026, 1, 1)));
        entwurf.put("zahlungsfristTage", 10);
        var id = anlegen(entwurf).get("id");
        post("/api/rechnungen/" + id + "/freigabe", Map.of("version", 0));

        client.get()
                .uri("/api/rechnungen?ueberfaellig=true&sortierung=FAELLIGKEIT_AUFSTEIGEND")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.inhalt[0].id")
                .isEqualTo(id)
                .jsonPath("$.inhalt[0].ueberfaellig")
                .isEqualTo(true)
                .jsonPath("$.inhalt[0].faelligAm")
                .isEqualTo("2026-01-11");
    }

    @Test
    void validiertPositionenUndWaehrung() {
        var entwurf = new java.util.HashMap<>(entwurf(LocalDate.of(2026, 9, 1)));
        entwurf.put(
                "positionen",
                List.of(Map.of("beschreibung", "Nichts", "menge", 0, "einzelpreis", 10, "mwstCode", "NORMAL")));
        client.post()
                .uri("/api/rechnungen")
                .headers(als(buchhaltung, mandant))
                .body(entwurf)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDIERUNG_FEHLGESCHLAGEN")
                .jsonPath("$.fehler[0].feld")
                .isEqualTo("positionen[0].menge");

        entwurf.put("waehrung", "USD");
        client.post()
                .uri("/api/rechnungen")
                .headers(als(buchhaltung, mandant))
                .body(entwurf)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void leserDarfKeineRechnungenAnlegen() {
        var leser = token("leser", mandant);
        client.post()
                .uri("/api/rechnungen")
                .headers(als(leser, mandant))
                .body(entwurf(LocalDate.of(2026, 9, 1)))
                .exchange()
                .expectStatus()
                .isForbidden();
        client.get()
                .uri("/api/rechnungen")
                .headers(als(leser, mandant))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void rechnungenAndererMandantenBleibenUnsichtbar() {
        var id = anlegen(entwurf(LocalDate.of(2026, 9, 1))).get("id");
        var anderer = mandantMitIban("Anderer Mandant GmbH");

        client.get()
                .uri("/api/rechnungen/" + id)
                .headers(als(token("admin", anderer), anderer))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void vorschauRechnetOhneZuSpeichern() {
        var anfrage = new java.util.HashMap<>(entwurf(LocalDate.of(2026, 9, 1)));
        anfrage.remove("kundeId");
        client.post()
                .uri("/api/rechnungen/vorschau")
                .headers(als(token("leser", mandant), mandant))
                .body(anfrage)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.summen.total")
                .isEqualTo(464.55)
                .jsonPath("$.summen.mwst.length()")
                .isEqualTo(2)
                .jsonPath("$.positionsbetraege")
                .isEqualTo(List.of(360.0, 73.5))
                .jsonPath("$.zahlteilSvg")
                .value(svg -> assertThat((String) svg).contains("<svg").contains("Vorschau"));

        client.get()
                .uri("/api/rechnungen")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(0);
    }

    @Test
    void mwstSaetzeAmStichtag() {
        client.get()
                .uri("/api/mwst-saetze?stichtag=2026-09-30")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(4)
                .jsonPath("$[?(@.code == 'NORMAL')].satz")
                .isEqualTo(List.of(8.10));
        client.get()
                .uri("/api/mwst-saetze")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(7);
    }

    private Map<String, Object> entwurf(LocalDate datum) {
        return entwurf(kunde, datum);
    }

    private static Map<String, Object> entwurf(UUID kunde, LocalDate datum) {
        return Map.of(
                "kundeId",
                kunde,
                "rechnungsdatum",
                datum.toString(),
                "waehrung",
                "CHF",
                "positionen",
                List.of(
                        Map.of(
                                "beschreibung",
                                "Montage",
                                "menge",
                                3,
                                "einheit",
                                "Std",
                                "einzelpreis",
                                "120.00",
                                "mwstCode",
                                "NORMAL"),
                        Map.of(
                                "beschreibung",
                                "Pflanzen",
                                "menge",
                                10,
                                "einheit",
                                "Stk",
                                "einzelpreis",
                                "7.35",
                                "mwstCode",
                                "REDUZIERT")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> anlegen(Map<String, Object> entwurf) {
        return client.post()
                .uri("/api/rechnungen")
                .headers(als(buchhaltung, mandant))
                .body(entwurf)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String uri, Map<String, Object> body) {
        return client.post()
                .uri(uri)
                .headers(als(buchhaltung, mandant))
                .body(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }

    @SuppressWarnings("unchecked")
    private static double summe(Map<String, Object> rechnung, String feld) {
        return ((Number) ((Map<String, Object>) rechnung.get("summen")).get(feld)).doubleValue();
    }
}
