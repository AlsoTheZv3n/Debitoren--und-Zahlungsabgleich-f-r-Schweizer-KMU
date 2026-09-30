package ch.kmu.debitoren.abgleich;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;

/**
 * Gate 3: Jedes camt-Testszenario unter {@code src/test/resources/camt} wird richtig zugeordnet. Ein Szenario
 * ist eine JSON-Datei mit Rechnungen, den Dateien, die importiert werden, und dem erwarteten Ergebnis;
 * die camt.054-Dateien liegen daneben. Jedes Szenario läuft für einen neuen Mandanten durch die ganze API,
 * so erhalten seine Rechnungen immer dieselben Nummern und Referenzen.
 */
class CamtSzenarienIT extends AbgleichTestBasis {

    record Szenario(
            String beschreibung,
            String konto,
            List<RechnungVorgabe> rechnungen,
            List<Import> importe,
            List<ZahlungErwartung> zahlungen,
            List<RechnungErwartung> rechnungenNachher) {}

    record RechnungVorgabe(String kunde, String betrag, String waehrung, Boolean storniert) {}

    record Import(String datei, int status, String code, Integer neu, Integer uebersprungen, Integer ignoriert) {}

    record ZahlungErwartung(
            String bankReferenz,
            String status,
            String klaerungsgrund,
            String rest,
            String waehrung,
            String auftraggeber,
            String mitteilung,
            List<ZuordnungErwartung> zuordnungen,
            List<VorschlagErwartung> vorschlaege) {}

    record ZuordnungErwartung(String rechnung, String betrag, String art, String regel) {}

    record VorschlagErwartung(String rechnung, String betrag, String regel, Boolean automatisch) {}

    record RechnungErwartung(String nummer, String status, String offen) {}

    static Stream<String> szenarien() throws IOException {
        return Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath:camt/*.json"))
                .map(Resource::getFilename)
                .sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("szenarien")
    void szenarioWirdRichtigZugeordnet(String datei) {
        var szenario = JSON.readValue(datei(datei), Szenario.class);
        var name = datei.replace(".json", "");
        var mandant = "SCOR".equals(szenario.konto()) ? mandantMitIban(name) : mandantMitQrIban(name);
        var token = token("buchhaltung", mandant);

        var kunden = new HashMap<String, UUID>();
        for (var vorgabe : szenario.rechnungen()) {
            var kunde = kunden.computeIfAbsent(vorgabe.kunde(), kundenname -> kundeAnlegen(token, mandant, kundenname));
            var offen = rechnungFreigeben(token, mandant, kunde, vorgabe.betrag(), vorgabe.waehrung());
            assertThat(betrag(offen, "summen", "total")).isEqualByComparingTo(vorgabe.betrag());
            if (Boolean.TRUE.equals(vorgabe.storniert())) {
                post(
                        token,
                        mandant,
                        "/api/rechnungen/" + offen.get("id").asString() + "/storno",
                        Map.of("version", offen.get("version").asLong(), "grund", "Doppelt erfasst"));
            }
        }
        for (var vorgabe : szenario.importe()) {
            importieren(token, mandant, vorgabe);
            aufAbgleichWarten(token, mandant);
        }

        var zahlungen = new HashMap<String, JsonNode>();
        get(token, mandant, "/api/zahlungen?groesse=200")
                .get("inhalt")
                .forEach(zahlung -> zahlungen.put(zahlung.get("bankReferenz").asString(), zahlung));
        assertThat(zahlungen.keySet())
                .as("Zahlungen nach dem Import")
                .containsExactlyInAnyOrderElementsOf(szenario.zahlungen().stream()
                        .map(ZahlungErwartung::bankReferenz)
                        .toList());
        for (var erwartet : szenario.zahlungen()) {
            pruefeZahlung(token, mandant, zahlungen.get(erwartet.bankReferenz()), erwartet);
        }
        for (var erwartet : szenario.rechnungenNachher()) {
            var rechnung = get(token, mandant, "/api/rechnungen?suche=" + erwartet.nummer())
                    .get("inhalt")
                    .get(0);
            assertThat(text(rechnung, "status"))
                    .as("Status von %s", erwartet.nummer())
                    .isEqualTo(erwartet.status());
            assertThat(betrag(rechnung, "offen"))
                    .as("Offen bei %s", erwartet.nummer())
                    .isEqualByComparingTo(erwartet.offen());
        }
    }

    private void pruefeZahlung(String token, UUID mandant, JsonNode zahlung, ZahlungErwartung erwartet) {
        var bezeichnung = "Zahlung " + erwartet.bankReferenz();
        assertThat(text(zahlung, "status")).as(bezeichnung).isEqualTo(erwartet.status());
        assertThat(text(zahlung, "klaerungsgrund"))
                .as("Klärungsgrund %s", bezeichnung)
                .isEqualTo(erwartet.klaerungsgrund());
        assertThat(betrag(zahlung, "rest")).as("Rest %s", bezeichnung).isEqualByComparingTo(erwartet.rest());
        if (erwartet.waehrung() != null) {
            assertThat(text(zahlung, "waehrung")).isEqualTo(erwartet.waehrung());
        }
        if (erwartet.auftraggeber() != null) {
            assertThat(text(zahlung, "auftraggeber")).isEqualTo(erwartet.auftraggeber());
        }
        if (erwartet.mitteilung() != null) {
            assertThat(text(zahlung, "mitteilung")).isEqualTo(erwartet.mitteilung());
        }
        var id = zahlung.get("id").asString();

        var zuordnungen = get(token, mandant, "/api/zahlungen/" + id + "/zuordnungen");
        var erwarteteZuordnungen =
                erwartet.zuordnungen() == null ? List.<ZuordnungErwartung>of() : erwartet.zuordnungen();
        assertThat(zuordnungen.size()).as("Zuordnungen %s", bezeichnung).isEqualTo(erwarteteZuordnungen.size());
        for (int i = 0; i < erwarteteZuordnungen.size(); i++) {
            var soll = erwarteteZuordnungen.get(i);
            var ist = zuordnungen.get(i);
            assertThat(text(ist, "rechnungsnummer")).isEqualTo(soll.rechnung());
            assertThat(betrag(ist, "betrag")).isEqualByComparingTo(soll.betrag());
            assertThat(text(ist, "art")).isEqualTo(soll.art());
            assertThat(text(ist, "regel")).isEqualTo(soll.regel());
        }

        if (erwartet.vorschlaege() != null) {
            var vorschlaege = get(token, mandant, "/api/zahlungen/" + id + "/vorschlaege");
            assertThat(vorschlaege.size())
                    .as("Vorschläge %s", bezeichnung)
                    .isEqualTo(erwartet.vorschlaege().size());
            for (int i = 0; i < erwartet.vorschlaege().size(); i++) {
                var soll = erwartet.vorschlaege().get(i);
                var ist = vorschlaege.get(i);
                assertThat(text(ist, "rechnungsnummer")).isEqualTo(soll.rechnung());
                assertThat(betrag(ist, "betrag")).isEqualByComparingTo(soll.betrag());
                assertThat(text(ist, "regel")).isEqualTo(soll.regel());
                assertThat(ist.get("automatisch").asBoolean()).isEqualTo(soll.automatisch());
            }
        }
    }

    private void importieren(String token, UUID mandant, Import vorgabe) {
        var ergebnis = JSON.readTree(hochladen(token, mandant, vorgabe.datei(), datei(vorgabe.datei()))
                .expectStatus()
                .isEqualTo(vorgabe.status())
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
        if (vorgabe.code() != null) {
            assertThat(text(ergebnis, "code")).isEqualTo(vorgabe.code());
            return;
        }
        assertThat(text(ergebnis, "dateiname")).isEqualTo(vorgabe.datei());
        assertThat(ergebnis.get("anzahlNeu").asInt()).as("neu").isEqualTo(vorgabe.neu());
        assertThat(ergebnis.get("anzahlUebersprungen").asInt())
                .as("übersprungen")
                .isEqualTo(vorgabe.uebersprungen());
        assertThat(ergebnis.get("anzahlIgnoriert").asInt()).as("ignoriert").isEqualTo(vorgabe.ignoriert());
    }
}
