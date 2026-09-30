package ch.kmu.debitoren.abgleich;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.kmu.debitoren.ApiTestBasis;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Hilfen für Tests rund um Import und Abgleich: Rechnungen freigeben, Dateien hochladen, auf den Abgleich warten. */
abstract class AbgleichTestBasis extends ApiTestBasis {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String RECHNUNGSDATUM = "2026-09-01";

    /** Freigegebene Rechnung mit einer Position ohne MWST, damit das Total dem Betrag entspricht. */
    JsonNode rechnungFreigeben(String token, UUID mandant, UUID kunde, String betrag, String waehrung) {
        var entwurf = Map.of(
                "kundeId", kunde,
                "rechnungsdatum", RECHNUNGSDATUM,
                "waehrung", waehrung,
                "positionen",
                        List.of(Map.of(
                                "beschreibung",
                                "Leistung gemäss Offerte",
                                "menge",
                                1,
                                "einzelpreis",
                                new BigDecimal(betrag),
                                "mwstCode",
                                "OHNE")));
        var angelegt = post(token, mandant, "/api/rechnungen", entwurf);
        return post(
                token,
                mandant,
                "/api/rechnungen/" + angelegt.get("id").asString() + "/freigabe",
                Map.of("version", angelegt.get("version").asLong()));
    }

    RestTestClient.ResponseSpec hochladen(String token, UUID mandant, String dateiname, byte[] inhalt) {
        var teile = new LinkedMultiValueMap<String, Object>();
        teile.add("datei", new ByteArrayResource(inhalt) {
            @Override
            public String getFilename() {
                return dateiname;
            }
        });
        return client.post()
                .uri("/api/zahlungsdateien")
                .headers(als(token, mandant))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(teile)
                .exchange();
    }

    /** Wartet, bis der Abgleich alle neuen Zahlungen bearbeitet hat; er läuft nach dem Commit asynchron. */
    void aufAbgleichWarten(String token, UUID mandant) {
        await().atMost(Duration.ofSeconds(30))
                .until(() -> get(token, mandant, "/api/zahlungen?status=OFFEN")
                                .get("anzahlElemente")
                                .asInt()
                        == 0);
    }

    JsonNode get(String token, UUID mandant, String pfad) {
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

    JsonNode post(String token, UUID mandant, String pfad, Object body) {
        return JSON.readTree(client.post()
                .uri(pfad)
                .headers(als(token, mandant))
                .body(body)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
    }

    static BigDecimal betrag(JsonNode knoten, String... pfad) {
        var wert = knoten;
        for (var teil : pfad) {
            wert = wert.get(teil);
        }
        return wert.decimalValue();
    }

    static String text(JsonNode knoten, String feld) {
        var wert = knoten.path(feld);
        return wert.isNull() || wert.isMissingNode() ? null : wert.asString();
    }

    static byte[] datei(String name) {
        try (InputStream strom = AbgleichTestBasis.class.getResourceAsStream("/camt/" + name)) {
            assertThat(strom).as("Testdatei %s", name).isNotNull();
            return strom.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
