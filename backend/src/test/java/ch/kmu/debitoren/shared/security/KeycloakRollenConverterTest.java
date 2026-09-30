package ch.kmu.debitoren.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRollenConverterTest {

    private final KeycloakRollenConverter konverter = new KeycloakRollenConverter();

    @Test
    void uebersetztBekannteRealmRollen() {
        var jwt = jwt(Map.of("roles", List.of("buchhaltung", "offline_access", "leser", "default-roles-debitoren")));
        assertThat(konverter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUCHHALTUNG", "ROLE_LESER");
    }

    @Test
    void ohneRealmRollenKeineAuthorities() {
        assertThat(konverter.convert(jwt(Map.of()))).isEmpty();
        assertThat(konverter.convert(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .claim("sub", "x")
                        .build()))
                .isEmpty();
    }

    @Test
    void systemRolleKannNichtAusDemTokenKommen() {
        assertThat(konverter.convert(jwt(Map.of("roles", List.of("system", "SYSTEM")))))
                .isEmpty();
    }

    private static Jwt jwt(Map<String, Object> realmZugriff) {
        return Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("realm_access", realmZugriff)
                .build();
    }
}
