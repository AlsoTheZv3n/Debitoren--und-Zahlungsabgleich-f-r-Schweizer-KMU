package ch.kmu.debitoren.stammdaten.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.security.KeycloakRollenConverter;
import ch.kmu.debitoren.shared.security.SecurityKonfiguration;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MandantController.class)
@Import({SecurityKonfiguration.class, MandantMapperImpl.class})
class MandantControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    MandantenAbfragen abfragen;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void liefertMandantenMitOrtUndReferenzart() {
        var id = UUID.fromString("019a0000-0000-7000-8000-000000000001");
        given(abfragen.erlaubteMandanten())
                .willReturn(List.of(new Mandant(
                        id,
                        "Berner Werkstatt AG",
                        Adresse.schweiz("Marktgasse", "28", "3011", "Bern"),
                        new Iban("CH5800791123000889012"),
                        new Iban("CH4431999123000889012"))));

        var antwort = mvc.get()
                .uri("/api/mandanten")
                .with(jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("leser"))))
                        .authorities(new KeycloakRollenConverter()))
                .exchange();

        assertThat(antwort).hasStatusOk();
        assertThat(antwort).bodyJson().extractingPath("$[0].id").isEqualTo(id.toString());
        assertThat(antwort).bodyJson().extractingPath("$[0].name").isEqualTo("Berner Werkstatt AG");
        assertThat(antwort).bodyJson().extractingPath("$[0].ort").isEqualTo("Bern");
        assertThat(antwort).bodyJson().extractingPath("$[0].referenzart").isEqualTo("QR");
    }
}
