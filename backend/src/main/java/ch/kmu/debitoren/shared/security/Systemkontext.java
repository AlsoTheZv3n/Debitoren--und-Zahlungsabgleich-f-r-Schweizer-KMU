package ch.kmu.debitoren.shared.security;

import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Führt Arbeit ohne angemeldeten Benutzer als System aus, etwa im Seeder oder in geplanten Jobs. Die Rolle
 * {@value #AUTHORITY} steht in der Hierarchie über Admin und kommt nie aus einem Token.
 */
public final class Systemkontext {

    public static final String BENUTZER = "system";
    public static final String AUTHORITY = "ROLE_SYSTEM";

    private Systemkontext() {}

    /**
     * Arbeit für einen Mandanten als System, etwa in einem Ereignis-Empfänger. Der Mandant ist gesetzt,
     * bevor die aufgerufenen Use Cases ihre Transaktion öffnen.
     */
    public static void fuerMandant(UUID mandantId, Runnable aktion) {
        ausfuehren(() -> MandantKontext.ausfuehrenFuer(mandantId, aktion));
    }

    public static void ausfuehren(Runnable aktion) {
        ausfuehren(() -> {
            aktion.run();
            return null;
        });
    }

    public static <T> T ausfuehren(Supplier<T> aktion) {
        var vorher = SecurityContextHolder.getContext();
        var kontext = SecurityContextHolder.createEmptyContext();
        kontext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                BENUTZER, null, List.of(new SimpleGrantedAuthority(AUTHORITY))));
        SecurityContextHolder.setContext(kontext);
        try {
            return aktion.get();
        } finally {
            SecurityContextHolder.setContext(vorher);
        }
    }
}
