package ch.kmu.debitoren.stammdaten;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.ApiTestBasis;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KundenIT extends ApiTestBasis {

    private UUID mandant;
    private String buchhaltung;

    @BeforeEach
    void mandantAnlegen() {
        mandant = mandantMitQrIban("Kunden Test AG");
        buchhaltung = token("buchhaltung", mandant);
    }

    @Test
    void vergibtKundennummernFortlaufend() {
        var erster = anlegen(kunde("Keller Haustechnik AG"));
        var zweiter = anlegen(kunde("Gasser Treuhand"));

        assertThat(erster.get("kundennummer")).isEqualTo("K-10001");
        assertThat(zweiter.get("kundennummer")).isEqualTo("K-10002");
        assertThat(erster.get("version")).isEqualTo(0);
    }

    @Test
    void eigeneKundennummerIstEindeutig() {
        var daten = new HashMap<>(kunde("Lüthi Bau AG"));
        daten.put("kundennummer", "L-001");
        assertThat(anlegen(daten).get("kundennummer")).isEqualTo("L-001");

        client.post()
                .uri("/api/kunden")
                .headers(als(buchhaltung, mandant))
                .body(daten)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("KUNDENNUMMER_VERGEBEN");
    }

    @Test
    void suchtInNameNummerUndOrtMitSeiten() {
        anlegen(kunde("Brunner Malerei"));
        anlegen(kunde("Keller Haustechnik AG"));

        client.get()
                .uri("/api/kunden?suche=keller")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(1)
                .jsonPath("$.inhalt[0].name")
                .isEqualTo("Keller Haustechnik AG");
        client.get()
                .uri("/api/kunden?suche=ZOLLI&groesse=1")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(2)
                .jsonPath("$.anzahlSeiten")
                .isEqualTo(2)
                .jsonPath("$.inhalt[0].name")
                .isEqualTo("Brunner Malerei");
    }

    @Test
    void aendertMitVersionspruefung() {
        var kunde = anlegen(kunde("Gasser Treuhand"));
        var id = kunde.get("id");
        var aenderung = new HashMap<>(kunde("Gasser Treuhand GmbH"));
        aenderung.put("zahlungsfristTage", 10);
        aenderung.put("version", 0);

        client.put()
                .uri("/api/kunden/" + id)
                .headers(als(buchhaltung, mandant))
                .body(aenderung)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.name")
                .isEqualTo("Gasser Treuhand GmbH")
                .jsonPath("$.zahlungsfristTage")
                .isEqualTo(10)
                .jsonPath("$.version")
                .isEqualTo(1)
                .jsonPath("$.kundennummer")
                .isEqualTo(kunde.get("kundennummer"));

        client.put()
                .uri("/api/kunden/" + id)
                .headers(als(buchhaltung, mandant))
                .body(aenderung)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("GLEICHZEITIG_GEAENDERT");
    }

    @Test
    void validiertEingaben() {
        var daten = new HashMap<>(kunde(" "));
        daten.put("land", "ch");
        daten.put("email", "keine-adresse");

        client.post()
                .uri("/api/kunden")
                .headers(als(buchhaltung, mandant))
                .body(daten)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDIERUNG_FEHLGESCHLAGEN")
                .jsonPath("$.fehler.length()")
                .isEqualTo(3);

        client.get()
                .uri("/api/kunden?seite=-1")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDIERUNG_FEHLGESCHLAGEN");
    }

    @Test
    void leserDarfNurLesen() {
        var leser = token("leser", mandant);
        client.post()
                .uri("/api/kunden")
                .headers(als(leser, mandant))
                .body(kunde("Nicht erlaubt AG"))
                .exchange()
                .expectStatus()
                .isForbidden();
        client.get()
                .uri("/api/kunden")
                .headers(als(leser, mandant))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void kundenAndererMandantenBleibenUnsichtbar() {
        var id = anlegen(kunde("Nur für A AG")).get("id");
        var anderer = mandantMitIban("Anderer Mandant GmbH");
        var fremd = token("buchhaltung", anderer);

        client.get()
                .uri("/api/kunden/" + id)
                .headers(als(fremd, anderer))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("KUNDE_NICHT_GEFUNDEN");
        client.get()
                .uri("/api/kunden")
                .headers(als(fremd, anderer))
                .exchange()
                .expectBody()
                .jsonPath("$.anzahlElemente")
                .isEqualTo(0);
    }

    @Test
    void ohneMandantKeineKunden() {
        var zwei = token("buchhaltung", mandant, mandantMitIban("Zweiter GmbH"));
        client.get()
                .uri("/api/kunden")
                .headers(kopf -> kopf.setBearerAuth(zwei))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("MANDANT_FEHLT");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> anlegen(Map<String, Object> daten) {
        return client.post()
                .uri("/api/kunden")
                .headers(als(buchhaltung, mandant))
                .body(daten)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }
}
