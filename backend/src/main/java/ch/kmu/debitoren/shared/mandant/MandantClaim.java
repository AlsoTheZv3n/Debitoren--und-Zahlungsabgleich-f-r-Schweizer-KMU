package ch.kmu.debitoren.shared.mandant;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Liest die erlaubten Mandanten aus dem Claim {@code mandant_id}. Keycloak liefert einen einzelnen String
 * oder, bei mehreren Mandanten, eine Liste. Ungültige Einträge werden ignoriert.
 */
public final class MandantClaim {

    public static final String NAME = "mandant_id";

    private MandantClaim() {}

    public static List<UUID> lesen(Jwt jwt) {
        Object claim = jwt.getClaims().get(NAME);
        Stream<?> werte = switch (claim) {
            case null -> Stream.empty();
            case Collection<?> liste -> liste.stream();
            default -> Stream.of(claim);
        };
        var mandanten = new LinkedHashSet<UUID>();
        werte.map(Object::toString)
                .map(MandantClaim::alsUuid)
                .flatMap(Optional::stream)
                .forEach(mandanten::add);
        return List.copyOf(mandanten);
    }

    static Optional<UUID> alsUuid(String wert) {
        try {
            return Optional.of(UUID.fromString(wert.strip()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
