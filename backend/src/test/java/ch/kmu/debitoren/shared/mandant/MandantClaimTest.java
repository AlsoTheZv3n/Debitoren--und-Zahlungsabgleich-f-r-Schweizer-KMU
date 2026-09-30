package ch.kmu.debitoren.shared.mandant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class MandantClaimTest {

    private static final UUID A = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID B = UUID.fromString("019a0000-0000-7000-8000-000000000002");

    @Test
    void liestEinzelnenString() {
        assertThat(MandantClaim.lesen(jwt(A.toString()))).containsExactly(A);
    }

    @Test
    void liestListeInReihenfolgeOhneDoppelte() {
        assertThat(MandantClaim.lesen(jwt(List.of(B.toString(), A.toString(), B.toString()))))
                .containsExactly(B, A);
    }

    @Test
    void ignoriertUngueltigeEintraege() {
        assertThat(MandantClaim.lesen(jwt(List.of("kein-uuid", A.toString())))).containsExactly(A);
    }

    @Test
    void liefertLeereListeOhneClaim() {
        var jwt =
                Jwt.withTokenValue("t").header("alg", "none").claim("sub", "x").build();
        assertThat(MandantClaim.lesen(jwt)).isEmpty();
    }

    private static Jwt jwt(Object mandanten) {
        return Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim(MandantClaim.NAME, mandanten)
                .issuedAt(Instant.now())
                .build();
    }
}
