package ch.kmu.debitoren.stammdaten;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.ApiTestBasis;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Einstellungen der Administration: Mandant und MWST-Sätze, mit Rollen, Versionen und Audit-Log. */
class EinstellungenIT extends ApiTestBasis {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private UUID mandant;
    private String admin;

    @BeforeEach
    void vorbereiten() {
        mandant = mandantMitQrIban("Einstellungen Test AG");
        admin = token("admin", mandant);
    }

    @Test
    void adminAendertMandantUndNeueRechnungenFolgenDemKonto() {
        var gelesen = get(token("leser", mandant), "/api/mandanten/aktuell");
        assertThat(gelesen.get("referenzart").asString()).isEqualTo("QR");
        assertThat(gelesen.get("qrIban").asString()).isEqualTo(QR_IBAN);

        var aenderung = new HashMap<String, Object>(Map.of(
                "name", "Einstellungen Test AG",
                "strasse", "Bahnhofstrasse",
                "hausnummer", "1",
                "plz", "3011",
                "ort", "Bern",
                "land", "CH",
                "iban", "CH58 0079 1123 0008 8901 2",
                "email", "rechnungen@einstellungen.example",
                "rundungFuenfRappen", false,
                "version", gelesen.get("version").asLong()));
        client.put()
                .uri("/api/mandanten/aktuell")
                .headers(als(token("buchhaltung", mandant), mandant))
                .body(aenderung)
                .exchange()
                .expectStatus()
                .isForbidden();

        var geaendert = put("/api/mandanten/aktuell", aenderung);
        assertThat(geaendert.get("referenzart").asString()).isEqualTo("SCOR");
        assertThat(geaendert.get("qrIban").isNull()).isTrue();
        assertThat(geaendert.get("iban").asString()).isEqualTo("CH5800791123000889012");
        assertThat(geaendert.get("rundungFuenfRappen").asBoolean()).isFalse();
        assertThat(geaendert.get("version").asLong())
                .isGreaterThan(gelesen.get("version").asLong());

        // Veraltete Version und ungültige IBAN werden abgelehnt.
        client.put()
                .uri("/api/mandanten/aktuell")
                .headers(als(admin, mandant))
                .body(aenderung)
                .exchange()
                .expectStatus()
                .isEqualTo(409);
        aenderung.put("version", geaendert.get("version").asLong());
        aenderung.put("iban", "CH00 0000 0000 0000 0000 0");
        client.put()
                .uri("/api/mandanten/aktuell")
                .headers(als(admin, mandant))
                .body(aenderung)
                .exchange()
                .expectStatus()
                .isBadRequest();

        var log = get(admin, "/api/audit?objektart=MANDANT").get("inhalt");
        assertThat(log.size()).isEqualTo(1);
        assertThat(log.get(0).get("benutzer").asString()).isEqualTo("admin");
    }

    @Test
    void neuerMwstSatzBeendetDenBisherigen() {
        client.post()
                .uri("/api/mwst-saetze")
                .headers(als(token("buchhaltung", mandant), mandant))
                .body(Map.of("code", "NORMAL", "satz", new BigDecimal("8.50"), "gueltigAb", "2028-01-01"))
                .exchange()
                .expectStatus()
                .isForbidden();

        var neu = JSON.readTree(client.post()
                .uri("/api/mwst-saetze")
                .headers(als(admin, mandant))
                .body(Map.of("code", "NORMAL", "satz", new BigDecimal("8.50"), "gueltigAb", "2028-01-01"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        assertThat(neu.get("satz").decimalValue()).isEqualByComparingTo("8.50");
        assertThat(neu.get("gueltigBis").isNull()).isTrue();

        var am31Dezember = get(admin, "/api/mwst-saetze?stichtag=2027-12-31");
        assertThat(satz(am31Dezember, "NORMAL")).isEqualByComparingTo("8.10");
        var amNeujahr = get(admin, "/api/mwst-saetze?stichtag=2028-01-01");
        assertThat(satz(amNeujahr, "NORMAL")).isEqualByComparingTo("8.50");

        client.post()
                .uri("/api/mwst-saetze")
                .headers(als(admin, mandant))
                .body(Map.of("code", "NORMAL", "satz", new BigDecimal("8.30"), "gueltigAb", "2027-07-01"))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    private static BigDecimal satz(JsonNode saetze, String code) {
        for (var satz : saetze) {
            if (satz.get("code").asString().equals(code)) {
                return satz.get("satz").decimalValue();
            }
        }
        throw new AssertionError("Kein Satz " + code);
    }

    private JsonNode get(String token, String pfad) {
        return JSON.readTree(client.get()
                .uri(pfad)
                .headers(als(token, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
    }

    private JsonNode put(String pfad, Object body) {
        return JSON.readTree(client.put()
                .uri(pfad)
                .headers(als(admin, mandant))
                .body(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
    }
}
