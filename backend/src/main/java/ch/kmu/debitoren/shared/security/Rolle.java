package ch.kmu.debitoren.shared.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * Fachliche Rollen aus Keycloak. Die Hierarchie steht in {@link SecurityKonfiguration}: Admin darf alles
 * der Buchhaltung, Buchhaltung alles der Leser.
 */
public enum Rolle {
    ADMIN("admin"),
    BUCHHALTUNG("buchhaltung"),
    LESER("leser");

    private final String keycloakName;

    Rolle(String keycloakName) {
        this.keycloakName = keycloakName;
    }

    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<Rolle> ausKeycloak(String name) {
        return Arrays.stream(values())
                .filter(rolle -> rolle.keycloakName.equals(name))
                .findFirst();
    }

    public static Optional<Rolle> ausAuthority(String authority) {
        return Arrays.stream(values())
                .filter(rolle -> rolle.authority().equals(authority))
                .findFirst();
    }
}
