package ch.kmu.debitoren;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.mandant.MandantFilter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Die ganze Anwendung mit PostgreSQL, Flyway, Security-Kette und Actuator. Tokens sind signiert wie von
 * Keycloak; den Weg über den echten Keycloak prüft der E2E-Test.
 */
class DebitorenApplicationIT extends ApiTestBasis {

    @LocalManagementPort
    int managementPort;

    private UUID zuerich;
    private UUID aarau;
    private UUID fremd;

    @BeforeEach
    void mandantenAnlegen() {
        zuerich = mandant("Zürcher Treuhand AG", "Zürich");
        aarau = mandant("Aargauer Schreinerei GmbH", "Aarau");
        fremd = mandant("Fremde Firma AG", "Basel");
    }

    @Test
    void healthUndPrometheusSindAufDemManagementPortOhneAnmeldungErreichbar() {
        var management = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + managementPort)
                .build();

        management
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
        management
                .get()
                .uri("/actuator/health/readiness")
                .exchange()
                .expectStatus()
                .isOk();
        management.get().uri("/actuator/prometheus").exchange().expectStatus().isOk();
        // Nicht freigegebene Endpunkte bleiben hinter der Anmeldung.
        management.get().uri("/actuator/env").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void apiOhneTokenAntwortet401() {
        client.get()
                .uri("/api/mandanten")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .exists(HttpHeaders.WWW_AUTHENTICATE)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("NICHT_ANGEMELDET");
    }

    @Test
    void tokenFuerEineAndereAudienceWirdAbgelehnt() {
        client.get()
                .uri("/api/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(TestTokens.token("admin", List.of("admin"), List.of(), "account")))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void meLiefertWirksameRollen() {
        client.get()
                .uri("/api/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(TestTokens.token("admin", List.of("admin"), List.of(zuerich))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.benutzername")
                .isEqualTo("admin")
                .jsonPath("$.rollen")
                .isEqualTo(List.of("ADMIN", "BUCHHALTUNG", "LESER"))
                .jsonPath("$.mandanten")
                .isEqualTo(List.of(zuerich.toString()));
    }

    @Test
    void mandantenlisteEnthaeltNurMandantenAusDemTokenNachNameSortiert() {
        client.get()
                .uri("/api/mandanten")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(TestTokens.token("leser", List.of("leser"), List.of(zuerich, aarau))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(2)
                .jsonPath("$[0].name")
                .isEqualTo("Aargauer Schreinerei GmbH")
                .jsonPath("$[1].name")
                .isEqualTo("Zürcher Treuhand AG")
                .jsonPath("$[1].referenzart")
                .isEqualTo("SCOR");
    }

    @Test
    void fremderMandantImHeaderWirdAbgelehnt() {
        client.get()
                .uri("/api/mandanten")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(TestTokens.token("leser", List.of("leser"), List.of(zuerich))))
                .header(MandantFilter.HEADER, fremd.toString())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("MANDANT_NICHT_ERLAUBT");
    }

    @Test
    void ohneFachrolleVerweigertDerUseCaseDenZugriff() {
        client.get()
                .uri("/api/mandanten")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(TestTokens.token("gast", List.of("offline_access"), List.of(zuerich))))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("ZUGRIFF_VERWEIGERT");
    }

    @Test
    void openApiBeschreibungIstImTestprofilErreichbar() {
        client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/api/me'].get.operationId")
                .isEqualTo("aktuellerBenutzer")
                .jsonPath("$.paths['/api/mandanten'].get.operationId")
                .isEqualTo("mandantenAuflisten")
                .jsonPath("$.paths['/api/me'].get.responses.default.content['application/problem+json'].schema.$ref")
                .isEqualTo("#/components/schemas/Problem")
                .jsonPath("$.components.schemas.Problem.required")
                .isEqualTo(List.of("code", "status", "title"));
    }

    @Test
    void flywayHatDieMigrationenAusgefuehrt() {
        var versionen = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versionen).contains("001");
    }

    private UUID mandant(String name, String ort) {
        var id = Ids.neu();
        jdbc.update(
                "insert into mandant (id, name, plz, ort, iban) values (?, ?, '8000', ?, 'CH9300762011623852957')",
                id,
                name,
                ort);
        return id;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
