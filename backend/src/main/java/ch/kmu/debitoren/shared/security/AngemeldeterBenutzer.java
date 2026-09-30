package ch.kmu.debitoren.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Name des Benutzers, der gerade arbeitet, etwa für «importiert von» oder «zugeordnet von». */
public final class AngemeldeterBenutzer {

    private AngemeldeterBenutzer() {}

    /**
     * Benutzername aus dem Token ({@code preferred_username}), im {@link Systemkontext} {@value
     * Systemkontext#BENUTZER}.
     */
    public static String name() {
        var anmeldung = SecurityContextHolder.getContext().getAuthentication();
        if (anmeldung == null
                || anmeldung.getName() == null
                || anmeldung.getName().isBlank()) {
            throw new IllegalStateException("Kein angemeldeter Benutzer");
        }
        return anmeldung.getName();
    }
}
