package ch.kmu.debitoren.shared.mandant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class MandantKontextTest {

    private static final UUID A = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID B = UUID.fromString("019a0000-0000-7000-8000-000000000002");

    @AfterEach
    void aufraeumen() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void istAusserhalbEinesAblaufsLeer() {
        assertThat(MandantKontext.aktuell()).isEmpty();
        assertThatThrownBy(MandantKontext::erforderlich)
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(AllgemeinerFehlercode.MANDANT_FEHLT));
    }

    @Test
    void giltNurInnerhalbDesAblaufs() {
        MandantKontext.ausfuehrenFuer(
                A, () -> assertThat(MandantKontext.erforderlich()).isEqualTo(A));
        assertThat(MandantKontext.aktuell()).isEmpty();
    }

    @Test
    void verschachtelteAblaeufeUeberdeckenUndStellenWiederHer() {
        var innen = MandantKontext.berechnenFuer(A, () -> {
            var b = MandantKontext.berechnenFuer(B, MandantKontext::erforderlich);
            assertThat(MandantKontext.erforderlich()).isEqualTo(A);
            return b;
        });
        assertThat(innen).isEqualTo(B);
    }

    @Test
    void erlaubteMandantenKommenAusDemToken() {
        assertThat(MandantKontext.erlaubteMandanten()).isEmpty();
        var jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim(MandantClaim.NAME, List.of(A.toString(), B.toString()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        assertThat(MandantKontext.erlaubteMandanten()).containsExactly(A, B);
    }

    @Test
    void tenantResolverNutztDenKontextOderKeinenMandanten() {
        var resolver = new MandantTenantResolver();
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(MandantTenantResolver.KEIN_MANDANT);
        MandantKontext.ausfuehrenFuer(
                B, () -> assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(B));
        assertThat(resolver.validateExistingCurrentSessions()).isFalse();
    }
}
