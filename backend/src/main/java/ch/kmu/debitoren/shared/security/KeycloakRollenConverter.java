package ch.kmu.debitoren.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Übersetzt die Realm-Rollen aus {@code realm_access.roles} in Spring-Authorities. Rollen, die die
 * Anwendung nicht kennt, fallen weg.
 */
public class KeycloakRollenConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (!(jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmZugriff)
                || !(realmZugriff.get("roles") instanceof Collection<?> rollen)) {
            return List.of();
        }
        return rollen.stream()
                .map(Object::toString)
                .map(Rolle::ausKeycloak)
                .flatMap(Optional::stream)
                .distinct()
                .<GrantedAuthority>map(rolle -> new SimpleGrantedAuthority(rolle.authority()))
                .toList();
    }
}
