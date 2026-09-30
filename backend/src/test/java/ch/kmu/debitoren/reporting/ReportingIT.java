package ch.kmu.debitoren.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.ApiTestBasis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Kennzahlen, Altersstruktur, Zahlungseingänge und Audit-Log über die API, getrennt nach Mandant. */
class ReportingIT extends ApiTestBasis {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalDate HEUTE = LocalDate.now(ZoneId.of("Europe/Zurich"));

    private UUID mandant;
    private String buchhaltung;
    private String admin;

    @BeforeEach
    void vorbereiten() {
        mandant = mandantMitQrIban("Reporting Test AG");
        buchhaltung = token("buchhaltung", mandant);
        admin = token("admin", mandant);
    }

    @Test
    void kennzahlenUndAltersstrukturZaehlenNurDenEigenenMandanten() {
        var kunde = kundeAnlegen(buchhaltung, mandant, "Keller Haustechnik AG");
        rechnung(kunde, HEUTE.minusDays(5), "100.00");
        rechnung(kunde, HEUTE.minusDays(50), "200.00");
        rechnung(kunde, HEUTE.minusDays(120), "300.00");
        entwurf(kunde, HEUTE, "50.00");

        // Ein anderer Mandant mit eigenen Rechnungen darf nichts verändern.
        var fremd = mandantMitQrIban("Fremd AG");
        var fremdeBuchhaltung = token("buchhaltung", fremd);
        var fremderKunde = kundeAnlegen(fremdeBuchhaltung, fremd, "Fremdkunde");
        client.post()
                .uri("/api/rechnungen")
                .headers(als(fremdeBuchhaltung, fremd))
                .body(entwurfDaten(fremderKunde, HEUTE.minusDays(200), "9999.00"))
                .exchange()
                .expectStatus()
                .isCreated();

        var uebersicht = get(buchhaltung, "/api/reporting/uebersicht");
        assertThat(uebersicht.get("stichtag").asString()).isEqualTo(HEUTE.toString());
        var offen = uebersicht.get("offen").get(0);
        assertThat(offen.get("waehrung").asString()).isEqualTo("CHF");
        assertThat(offen.get("betrag").decimalValue()).isEqualByComparingTo("600.00");
        assertThat(offen.get("anzahl").asLong()).isEqualTo(3);
        // Fällig nach 10 Tagen: überfällig sind die Rechnungen von vor 50 und 120 Tagen.
        var ueberfaellig = uebersicht.get("ueberfaellig").get(0);
        assertThat(ueberfaellig.get("betrag").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(ueberfaellig.get("anzahl").asLong()).isEqualTo(2);
        assertThat(uebersicht.get("entwuerfe").asLong()).isEqualTo(1);
        assertThat(uebersicht.get("mahnvorschlaege").asLong()).isZero();
        assertThat(uebersicht.get("letzterMahnlauf").isNull()).isTrue();

        var struktur = get(buchhaltung, "/api/reporting/altersstruktur").get("waehrungen");
        assertThat(struktur.size()).isEqualTo(1);
        var chf = struktur.get(0);
        assertThat(chf.get("waehrung").asString()).isEqualTo("CHF");
        assertThat(chf.get("total").decimalValue()).isEqualByComparingTo("600.00");
        assertThat(betraege(chf.get("baender")))
                .containsExactly(
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("200.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("300.00"));
    }

    @Test
    void zahlungseingaengeHabenJedenMonatDesZeitraums() {
        var eingaenge = get(buchhaltung, "/api/reporting/zahlungseingaenge?monate=6");
        assertThat(eingaenge.size()).isEqualTo(6);
        assertThat(eingaenge.get(5).get("monat").asString())
                .isEqualTo(YearMonth.from(HEUTE).toString());
        assertThat(eingaenge.get(0).get("betrag").decimalValue()).isZero();
    }

    @Test
    void verlaufZeigtFreigabeUndStornoMitBenutzer() {
        var kunde = kundeAnlegen(buchhaltung, mandant, "Gasser Treuhand GmbH");
        var rechnung = rechnung(kunde, HEUTE.minusDays(3), "95.50");
        post(
                buchhaltung,
                "/api/rechnungen/" + rechnung.get("id").asString() + "/storno",
                Map.of("version", rechnung.get("version").asLong(), "grund", "Doppelt erfasst"));

        var verlauf = get(
                token("leser", mandant),
                "/api/audit/rechnung/" + rechnung.get("id").asString());
        assertThat(verlauf.size()).isEqualTo(3);
        var storno = verlauf.get(0);
        assertThat(storno.get("aktion").asString()).isEqualTo("GEAENDERT");
        assertThat(storno.get("benutzer").asString()).isEqualTo("buchhaltung");
        assertThat(storno.get("bezeichnung").asString())
                .isEqualTo(rechnung.get("rechnungsnummer").asString() + " · Gasser Treuhand GmbH");
        assertThat(aenderung(storno, "Status")).containsExactly("Offen", "Storniert");
        assertThat(aenderung(storno, "Stornogrund")).containsExactly(null, "Doppelt erfasst");
        var freigabe = verlauf.get(1);
        assertThat(aenderung(freigabe, "Status")).containsExactly("Entwurf", "Offen");
        assertThat(verlauf.get(2).get("aktion").asString()).isEqualTo("ANGELEGT");
    }

    @Test
    void ganzesLogNurFuerAdminsUndNachObjektartFilterbar() {
        var kunde = kundeAnlegen(buchhaltung, mandant, "Lüthi Bau AG");
        client.get()
                .uri("/api/audit")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isForbidden();

        var kunden = get(admin, "/api/audit?objektart=KUNDE").get("inhalt");
        assertThat(kunden.size()).isEqualTo(1);
        assertThat(kunden.get(0).get("objektId").asString()).isEqualTo(kunde.toString());
        assertThat(kunden.get(0).get("aktion").asString()).isEqualTo("ANGELEGT");
        assertThat(aenderung(kunden.get(0), "Name")).containsExactly(null, "Lüthi Bau AG");

        var vonBuchhaltung = get(admin, "/api/audit?benutzer=buchhaltung");
        assertThat(vonBuchhaltung.get("anzahlElemente").asLong()).isEqualTo(1);
        assertThat(get(admin, "/api/audit?benutzer=niemand")
                        .get("anzahlElemente")
                        .asLong())
                .isZero();
    }

    private JsonNode rechnung(UUID kunde, LocalDate datum, String betrag) {
        var entwurf = entwurf(kunde, datum, betrag);
        return post(
                buchhaltung,
                "/api/rechnungen/" + entwurf.get("id").asString() + "/freigabe",
                Map.of("version", entwurf.get("version").asLong()));
    }

    private JsonNode entwurf(UUID kunde, LocalDate datum, String betrag) {
        return post(buchhaltung, "/api/rechnungen", entwurfDaten(kunde, datum, betrag));
    }

    private static Map<String, Object> entwurfDaten(UUID kunde, LocalDate datum, String betrag) {
        return Map.of(
                "kundeId",
                kunde,
                "rechnungsdatum",
                datum.toString(),
                "zahlungsfristTage",
                10,
                "waehrung",
                "CHF",
                "positionen",
                List.of(Map.of(
                        "beschreibung",
                        "Wartung",
                        "menge",
                        1,
                        "einzelpreis",
                        new BigDecimal(betrag),
                        "mwstCode",
                        "OHNE")));
    }

    private static List<BigDecimal> betraege(JsonNode baender) {
        var liste = new java.util.ArrayList<BigDecimal>();
        baender.forEach(band -> liste.add(band.get("betrag").decimalValue().stripTrailingZeros()));
        return liste.stream()
                .map(betrag -> betrag.signum() == 0 ? BigDecimal.ZERO : betrag.setScale(2))
                .toList();
    }

    private static List<String> aenderung(JsonNode eintrag, String feld) {
        for (var aenderung : eintrag.get("aenderungen")) {
            if (aenderung.get("feld").asString().equals(feld)) {
                return java.util.Arrays.asList(text(aenderung, "vorher"), text(aenderung, "nachher"));
            }
        }
        throw new AssertionError("Keine Änderung am Feld " + feld + " in " + eintrag);
    }

    private static String text(JsonNode knoten, String feld) {
        var wert = knoten.path(feld);
        return wert.isNull() || wert.isMissingNode() ? null : wert.asString();
    }

    private JsonNode get(String token, String pfad) {
        return JSON.readTree(
                antwort(client.get().uri(pfad).headers(als(token, mandant)).exchange()));
    }

    private JsonNode post(String token, String pfad, Object body) {
        return JSON.readTree(antwort(
                client.post().uri(pfad).headers(als(token, mandant)).body(body).exchange()));
    }

    private static String antwort(RestTestClient.ResponseSpec antwort) {
        return antwort.expectStatus()
                .is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }
}
