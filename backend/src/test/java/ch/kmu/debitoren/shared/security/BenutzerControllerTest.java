package ch.kmu.debitoren.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import ch.kmu.debitoren.shared.mandant.MandantClaim;
import ch.kmu.debitoren.shared.mandant.MandantFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BenutzerController.class)
@Import(SecurityKonfiguration.class)
class BenutzerControllerTest {

    private static final String MANDANT_A = "019a0000-0000-7000-8000-000000000001";
    private static final String MANDANT_B = "019a0000-0000-7000-8000-000000000002";

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void ohneTokenAntwortet401AlsProblemDetail() {
        var antwort = mvc.get().uri("/api/me").exchange();

        assertThat(antwort).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(antwort).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(antwort).headers().containsHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(antwort).bodyJson().extractingPath("$.code").isEqualTo("NICHT_ANGEMELDET");
        assertThat(antwort).bodyJson().extractingPath("$.status").isEqualTo(401);
    }

    @Test
    void adminErhaeltUeberDieHierarchieAlleRollen() {
        var antwort = mvc.get().uri("/api/me").with(token("admin", "admin")).exchange();

        assertThat(antwort).hasStatusOk();
        assertThat(antwort).bodyJson().extractingPath("$.benutzername").isEqualTo("admin");
        assertThat(antwort).bodyJson().extractingPath("$.anzeigename").isEqualTo("Test admin");
        assertThat(antwort)
                .bodyJson()
                .extractingPath("$.rollen")
                .asArray()
                .containsExactly("ADMIN", "BUCHHALTUNG", "LESER");
        assertThat(antwort).bodyJson().extractingPath("$.mandanten").asArray().containsExactly(MANDANT_A, MANDANT_B);
    }

    @Test
    void buchhaltungIstAuchLeserAberNichtAdmin() {
        var antwort = mvc.get()
                .uri("/api/me")
                .with(token("buchhaltung", "buchhaltung"))
                .exchange();

        assertThat(antwort).bodyJson().extractingPath("$.rollen").asArray().containsExactly("BUCHHALTUNG", "LESER");
    }

    @Test
    void unbekannteRollenFallenWeg() {
        var antwort =
                mvc.get().uri("/api/me").with(token("gast", "offline_access")).exchange();

        assertThat(antwort).hasStatusOk();
        assertThat(antwort).bodyJson().extractingPath("$.rollen").asArray().isEmpty();
    }

    @Test
    void fremderMandantImHeaderWirdAbgelehnt() {
        var antwort = mvc.get()
                .uri("/api/me")
                .header(MandantFilter.HEADER, "019a0000-0000-7000-8000-00000000ffff")
                .with(token("leser", "leser"))
                .exchange();

        assertThat(antwort).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(antwort).bodyJson().extractingPath("$.code").isEqualTo("MANDANT_NICHT_ERLAUBT");
    }

    @Test
    void ungueltigerMandantImHeaderWirdAbgelehnt() {
        var antwort = mvc.get()
                .uri("/api/me")
                .header(MandantFilter.HEADER, "Berner Werkstatt")
                .with(token("leser", "leser"))
                .exchange();

        assertThat(antwort).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(antwort).bodyJson().extractingPath("$.code").isEqualTo("MANDANT_UNGUELTIG");
    }

    @Test
    void erlaubterMandantImHeaderWirdAkzeptiert() {
        var antwort = mvc.get()
                .uri("/api/me")
                .header(MandantFilter.HEADER, MANDANT_B)
                .with(token("leser", "leser"))
                .exchange();

        assertThat(antwort).hasStatusOk();
    }

    @Test
    void unbekannterPfadAntwortet404MitCode() {
        var antwort = mvc.get()
                .uri("/api/gibt-es-nicht")
                .with(token("leser", "leser"))
                .exchange();

        assertThat(antwort).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(antwort).bodyJson().extractingPath("$.code").isEqualTo("NICHT_GEFUNDEN");
    }

    private static JwtRequestPostProcessor token(String benutzer, String... rollen) {
        return jwt().jwt(j -> j.claim("preferred_username", benutzer)
                        .claim("name", "Test " + benutzer)
                        .claim("realm_access", Map.of("roles", List.of(rollen)))
                        .claim(MandantClaim.NAME, List.of(MANDANT_A, MANDANT_B)))
                .authorities(new KeycloakRollenConverter());
    }
}
