package ch.kmu.debitoren.stammdaten.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantClaim;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Use Cases mit einem Repository im Speicher; die Rollenprüfung steht in den Integrationstests. */
class MandantVerwaltungTest {

    private static final UUID A = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID B = UUID.fromString("019a0000-0000-7000-8000-000000000002");

    private final MandantenImSpeicher repository = new MandantenImSpeicher();
    private final List<Object> ereignisse = new java.util.ArrayList<>();
    private final MandantVerwaltung verwaltung = new MandantVerwaltung(repository, ereignisse::add);
    private final MandantenAbfragen abfragen = new MandantenAbfragen(repository);

    @AfterEach
    void aufraeumen() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void legtMandantAn() {
        var mandant = verwaltung.anlegen(neu(A, "Berner Werkstatt AG"));

        assertThat(mandant.getId()).isEqualTo(A);
        assertThat(ereignisse).containsExactly(new MandantAngelegt(A));
        assertThat(mandant.getMwstNummer()).isEqualTo("CHE-100.200.300 MWST");
        assertThat(mandant.isRundungFuenfRappen()).isTrue();
        assertThat(abfragen.existiert(A)).isTrue();
        assertThat(abfragen.existiert(B)).isFalse();
    }

    @Test
    void verweigertDoppelteId() {
        verwaltung.anlegen(neu(A, "Berner Werkstatt AG"));

        assertThatThrownBy(() -> verwaltung.anlegen(neu(A, "Nochmals")))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(AllgemeinerFehlercode.BEREITS_VORHANDEN));
    }

    @Test
    void liefertNurMandantenAusDemToken() {
        verwaltung.anlegen(neu(A, "Zürcher Firma"));
        verwaltung.anlegen(neu(B, "Aargauer Firma"));
        anmelden(List.of(A.toString()));

        assertThat(abfragen.erlaubteMandanten()).extracting(Mandant::getId).containsExactly(A);
    }

    @Test
    void ohneMandantImTokenKeineMandanten() {
        verwaltung.anlegen(neu(A, "Zürcher Firma"));
        anmelden(List.of());

        assertThat(abfragen.erlaubteMandanten()).isEmpty();
    }

    private static void anmelden(List<String> mandanten) {
        var jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim(MandantClaim.NAME, mandanten)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static NeuerMandant neu(UUID id, String name) {
        return new NeuerMandant(
                id,
                name,
                Adresse.schweiz("Weg", "1", "3000", "Bern"),
                new Iban("CH9300762011623852957"),
                null,
                "CHE-100.200.300 MWST",
                "info@firma.example");
    }

    private static final class MandantenImSpeicher implements MandantRepository {

        private final Map<UUID, Mandant> mandanten = new LinkedHashMap<>();

        @Override
        public List<Mandant> findeAlle(Collection<UUID> ids) {
            return mandanten.values().stream()
                    .filter(m -> ids.contains(m.getId()))
                    .sorted(Comparator.comparing(Mandant::getName))
                    .toList();
        }

        @Override
        public java.util.Optional<Mandant> finde(UUID id) {
            return java.util.Optional.ofNullable(mandanten.get(id));
        }

        @Override
        public boolean existiert(UUID id) {
            return mandanten.containsKey(id);
        }

        @Override
        public List<UUID> alleIds() {
            return mandanten.values().stream()
                    .sorted(Comparator.comparing(Mandant::getName))
                    .map(Mandant::getId)
                    .toList();
        }

        @Override
        public Mandant speichern(Mandant mandant) {
            mandanten.put(mandant.getId(), mandant);
            return mandant;
        }
    }
}
