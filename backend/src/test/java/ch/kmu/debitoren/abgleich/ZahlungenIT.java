package ch.kmu.debitoren.abgleich;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Import über die API, manuelle Zuordnung in der Klärungsliste, Aufheben, Konflikte und Rollen. */
class ZahlungenIT extends AbgleichTestBasis {

    private UUID mandant;
    private String buchhaltung;
    private UUID gasser;

    @BeforeEach
    void vorbereiten() {
        mandant = mandantMitQrIban("Zahlungen Test AG");
        buchhaltung = token("buchhaltung", mandant);
        gasser = kundeAnlegen(buchhaltung, mandant, "Gasser Treuhand GmbH");
    }

    @Test
    void vorschlagZuordnenUndWiederAufheben() {
        var rechnung = rechnungFreigeben(buchhaltung, mandant, gasser, "95.50", "CHF");
        var rechnungId = rechnung.get("id").asString();
        importieren("ohne-referenz.xml");
        var zahlung = get(buchhaltung, mandant, "/api/zahlungen?status=KLAERUNG")
                .get("inhalt")
                .get(0);
        var zahlungId = zahlung.get("id").asString();
        var version = zahlung.get("version").asLong();

        // Leser sehen die Klärungsliste, ordnen aber nichts zu.
        var leser = token("leser", mandant);
        assertThat(get(leser, mandant, "/api/zahlungen/" + zahlungId + "/vorschlaege")
                        .size())
                .isEqualTo(1);
        zuordnen(leser, zahlungId, Map.of("version", version, "rechnungId", rechnungId))
                .expectStatus()
                .isForbidden();

        zuordnen(buchhaltung, zahlungId, Map.of("version", version + 1, "rechnungId", rechnungId))
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("GLEICHZEITIG_GEAENDERT");
        zuordnen(buchhaltung, zahlungId, Map.of("version", version, "rechnungId", rechnungId, "betrag", 100))
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("BETRAG_UNGUELTIG");

        var zugeordnet =
                JSON.readTree(zuordnen(buchhaltung, zahlungId, Map.of("version", version, "rechnungId", rechnungId))
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody());
        assertThat(text(zugeordnet, "status")).isEqualTo("ZUGEORDNET");
        assertThat(text(zugeordnet, "klaerungsgrund")).isNull();
        assertThat(betrag(zugeordnet, "rest")).isZero();
        assertThat(zugeordnet.get("version").asLong()).isGreaterThan(version);
        var bezahlt = get(buchhaltung, mandant, "/api/rechnungen/" + rechnungId);
        assertThat(text(bezahlt, "status")).isEqualTo("BEZAHLT");
        assertThat(betrag(bezahlt, "offen")).isZero();

        zuordnen(
                        buchhaltung,
                        zahlungId,
                        Map.of("version", zugeordnet.get("version").asLong(), "rechnungId", rechnungId))
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("ZAHLUNG_BEREITS_ZUGEORDNET");

        var eingaenge = get(buchhaltung, mandant, "/api/zuordnungen?rechnungId=" + rechnungId);
        assertThat(eingaenge.size()).isEqualTo(1);
        var eingang = eingaenge.get(0);
        assertThat(text(eingang, "art")).isEqualTo("MANUELL");
        assertThat(text(eingang, "erstelltVon")).isEqualTo("buchhaltung");
        assertThat(text(eingang, "auftraggeber")).isEqualTo("GASSER TREUHAND");
        assertThat(text(eingang, "valuta")).isEqualTo("2026-09-15");

        client.delete()
                .uri("/api/zuordnungen/" + text(eingang, "id"))
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isNoContent();
        var aufgehoben = get(buchhaltung, mandant, "/api/zahlungen/" + zahlungId);
        assertThat(text(aufgehoben, "status")).isEqualTo("KLAERUNG");
        assertThat(text(aufgehoben, "klaerungsgrund")).isEqualTo("ZUORDNUNG_AUFGEHOBEN");
        assertThat(betrag(aufgehoben, "rest")).isEqualByComparingTo("95.50");
        var wiederOffen = get(buchhaltung, mandant, "/api/rechnungen/" + rechnungId);
        assertThat(text(wiederOffen, "status")).isEqualTo("OFFEN");
        assertThat(betrag(wiederOffen, "offen")).isEqualByComparingTo("95.50");
        client.delete()
                .uri("/api/zuordnungen/" + text(eingang, "id"))
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void eineZahlungAufZweiRechnungenVerteilen() {
        var erste = rechnungFreigeben(buchhaltung, mandant, gasser, "60.00", "CHF")
                .get("id")
                .asString();
        var zweite = rechnungFreigeben(buchhaltung, mandant, gasser, "35.50", "CHF")
                .get("id")
                .asString();
        importieren("ohne-referenz.xml");
        var zahlung = get(buchhaltung, mandant, "/api/zahlungen?status=KLAERUNG&suche=gasser")
                .get("inhalt")
                .get(0);
        var zahlungId = zahlung.get("id").asString();

        var teil = JSON.readTree(zuordnen(
                        buchhaltung,
                        zahlungId,
                        Map.of("version", zahlung.get("version").asLong(), "rechnungId", erste))
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        assertThat(text(teil, "klaerungsgrund")).isEqualTo("RESTBETRAG");
        assertThat(betrag(teil, "rest")).isEqualByComparingTo("35.50");

        var vorschlaege = get(buchhaltung, mandant, "/api/zahlungen/" + zahlungId + "/vorschlaege");
        assertThat(text(vorschlaege.get(0), "rechnungId")).isEqualTo(zweite);

        var fertig = JSON.readTree(zuordnen(
                        buchhaltung,
                        zahlungId,
                        Map.of("version", teil.get("version").asLong(), "rechnungId", zweite))
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        assertThat(text(fertig, "status")).isEqualTo("ZUGEORDNET");
        assertThat(get(buchhaltung, mandant, "/api/zahlungen/" + zahlungId + "/zuordnungen")
                        .size())
                .isEqualTo(2);
    }

    @Test
    void importprotokollUndOriginal() {
        rechnungFreigeben(buchhaltung, mandant, gasser, "95.50", "CHF");
        var inhalt = datei("ohne-referenz.xml");
        var importiert = JSON.readTree(hochladen(buchhaltung, mandant, "C:\\Downloads\\ohne-referenz.xml", inhalt)
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        assertThat(text(importiert, "dateiname")).isEqualTo("ohne-referenz.xml");
        assertThat(text(importiert, "importiertVon")).isEqualTo("buchhaltung");
        assertThat(importiert.get("unbearbeitet").asLong()).isEqualTo(1);
        aufAbgleichWarten(buchhaltung, mandant);

        var protokoll = get(token("leser", mandant), mandant, "/api/zahlungsdateien");
        assertThat(protokoll.get("anzahlElemente").asInt()).isEqualTo(1);
        var zeile = protokoll.get("inhalt").get(0);
        assertThat(text(zeile, "nachrichtId")).isEqualTo("MSG-OHNEREF-20260915");
        assertThat(zeile.get("inKlaerung").asLong()).isEqualTo(1);
        assertThat(zeile.get("zugeordnet").asLong()).isZero();
        assertThat(zeile.get("unbearbeitet").asLong()).isZero();

        var original = client.get()
                .uri("/api/zahlungsdateien/" + text(zeile, "id") + "/original")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueMatches("Content-Disposition", ".*ohne-referenz\\.xml.*")
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
        assertThat(original).isEqualTo(inhalt);

        var dieserDatei = get(buchhaltung, mandant, "/api/zahlungen?zahlungsdateiId=" + text(zeile, "id"));
        assertThat(dieserDatei.get("anzahlElemente").asInt()).isEqualTo(1);
    }

    @Test
    void lehntFremdeKontenUngueltigeDateienUndLeserAb() {
        var anderesKonto = mandantMitIban("Anderes Konto AG");
        var tokenAnderesKonto = token("buchhaltung", anderesKonto);
        hochladen(tokenAnderesKonto, anderesKonto, "exakte-zahlung.xml", datei("exakte-zahlung.xml"))
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("KONTO_UNBEKANNT");

        hochladen(buchhaltung, mandant, "notiz.txt", "keine Bankdatei".getBytes(StandardCharsets.UTF_8))
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("DATEI_UNGUELTIG");

        hochladen(token("leser", mandant), mandant, "exakte-zahlung.xml", datei("exakte-zahlung.xml"))
                .expectStatus()
                .isForbidden();

        assertThat(get(buchhaltung, mandant, "/api/zahlungsdateien")
                        .get("anzahlElemente")
                        .asInt())
                .isZero();
    }

    @Test
    void zahlungenAndererMandantenBleibenUnsichtbar() {
        rechnungFreigeben(buchhaltung, mandant, gasser, "95.50", "CHF");
        importieren("ohne-referenz.xml");
        var zahlungId = get(buchhaltung, mandant, "/api/zahlungen")
                .get("inhalt")
                .get(0)
                .get("id")
                .asString();

        var fremd = mandantMitQrIban("Fremd AG");
        var fremdToken = token("buchhaltung", fremd);
        assertThat(get(fremdToken, fremd, "/api/zahlungen")
                        .get("anzahlElemente")
                        .asInt())
                .isZero();
        client.get()
                .uri("/api/zahlungen/" + zahlungId)
                .headers(als(fremdToken, fremd))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private void importieren(String dateiname) {
        hochladen(buchhaltung, mandant, dateiname, datei(dateiname))
                .expectStatus()
                .isCreated();
        aufAbgleichWarten(buchhaltung, mandant);
    }

    private org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec zuordnen(
            String token, String zahlungId, Map<String, Object> anfrage) {
        return client.post()
                .uri("/api/zahlungen/" + zahlungId + "/zuordnungen")
                .headers(als(token, mandant))
                .body(anfrage)
                .exchange();
    }
}
