package ch.kmu.debitoren;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Signiert Tokens im Aufbau von Keycloak und prüft sie mit Aussteller und Audience wie in Produktion. So
 * durchlaufen Integrationstests den echten Security-Pfad, ohne dass Keycloak läuft.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestTokens {

    public static final String AUSSTELLER = "https://keycloak.test/realms/debitoren";
    public static final String AUDIENCE = "debitoren-api";

    private static final RSAKey SCHLUESSEL = neuerSchluessel();

    @Bean
    JwtDecoder jwtDecoder() throws JOSEException {
        var decoder =
                NimbusJwtDecoder.withPublicKey(SCHLUESSEL.toRSAPublicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(AUSSTELLER),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(AUDIENCE))));
        return decoder;
    }

    public static String token(String benutzer, List<String> rollen, List<UUID> mandanten) {
        return token(benutzer, rollen, mandanten, AUDIENCE);
    }

    public static String token(String benutzer, List<String> rollen, List<UUID> mandanten, String audience) {
        var jetzt = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(AUSSTELLER)
                .subject(UUID.nameUUIDFromBytes(benutzer.getBytes()).toString())
                .audience(audience)
                .issueTime(Date.from(jetzt))
                .expirationTime(Date.from(jetzt.plus(5, ChronoUnit.MINUTES)))
                .claim("preferred_username", benutzer)
                .claim("name", "Test " + benutzer)
                .claim("email", benutzer + "@example.ch")
                .claim("realm_access", Map.of("roles", rollen))
                .claim("mandant_id", mandanten.stream().map(UUID::toString).toList())
                .build();
        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(SCHLUESSEL.getKeyID())
                        .build(),
                claims);
        try {
            jwt.sign(new RSASSASigner(SCHLUESSEL));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }

    private static RSAKey neuerSchluessel() {
        try {
            return new RSAKeyGenerator(2048).keyID("test").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
