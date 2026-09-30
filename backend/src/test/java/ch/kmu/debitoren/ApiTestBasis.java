package ch.kmu.debitoren;

import ch.kmu.debitoren.mahnwesen.application.Mahnstufen;
import ch.kmu.debitoren.rechnung.application.MwstSaetze;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.mandant.MandantFilter;
import ch.kmu.debitoren.shared.security.Systemkontext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Gemeinsame Basis der API-Integrationstests: echte Anwendung mit PostgreSQL, Tokens wie von Keycloak.
 * Alle Unterklassen teilen sich einen Spring-Kontext und damit einen Datenbank-Container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestTokens.class, TestMailserver.class})
@AutoConfigureRestTestClient
@AutoConfigureMetrics
@ActiveProfiles("test")
public abstract class ApiTestBasis {

    public static final String QR_IBAN = "CH4431999123000889012";
    public static final String IBAN = "CH9300762011623852957";

    @Autowired
    protected RestTestClient client;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private MwstSaetze mwstSaetze;

    @Autowired
    private Mahnstufen mahnstufen;

    /** Mandant mit QR-IBAN, den gesetzlichen MWST-Sätzen und den Standard-Mahnstufen. */
    protected UUID mandantMitQrIban(String name) {
        return mandant(name, QR_IBAN);
    }

    /** Mandant mit normaler IBAN; seine Rechnungen tragen eine SCOR-Referenz. */
    protected UUID mandantMitIban(String name) {
        return mandant(name, null);
    }

    private UUID mandant(String name, String qrIban) {
        var id = Ids.neu();
        jdbc.update("""
                insert into mandant (id, name, strasse, hausnummer, plz, ort, iban, qr_iban, mwst_nummer)
                values (?, ?, 'Marktgasse', '28', '3011', 'Bern', ?, ?, 'CHE-100.200.300 MWST')
                """, id, name, qrIban == null ? IBAN : "CH5800791123000889012", qrIban);
        Systemkontext.fuerMandant(id, () -> {
            mwstSaetze.standardSaetzeAnlegen();
            mahnstufen.standardAnlegen();
        });
        return id;
    }

    protected static String token(String rolle, UUID... mandanten) {
        return TestTokens.token(rolle, List.of(rolle), List.of(mandanten));
    }

    /** Setzt Token und Mandanten-Header. */
    protected static java.util.function.Consumer<HttpHeaders> als(String token, UUID mandant) {
        return kopf -> {
            kopf.setBearerAuth(token);
            kopf.set(MandantFilter.HEADER, mandant.toString());
        };
    }

    protected static Map<String, Object> kunde(String name) {
        return Map.of(
                "name", name,
                "strasse", "Industriestrasse",
                "hausnummer", "7",
                "plz", "3052",
                "ort", "Zollikofen",
                "land", "CH",
                "email", "buchhaltung@beispiel.example",
                "zahlungsfristTage", 30);
    }

    @SuppressWarnings("unchecked")
    protected UUID kundeAnlegen(String token, UUID mandant, String name) {
        var antwort = client.post()
                .uri("/api/kunden")
                .headers(als(token, mandant))
                .body(kunde(name))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        return UUID.fromString((String) ((Map<String, Object>) antwort).get("id"));
    }
}
