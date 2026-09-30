package ch.kmu.debitoren.shared.mandant;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Mandant, für den der laufende Ablauf arbeitet. In einer Anfrage setzt ihn der {@link MandantFilter}, in
 * Jobs und im Seeder {@link #ausfuehrenFuer} oder {@link #berechnenFuer}.
 *
 * <p>Hibernate liest den Mandanten beim Öffnen der Session. Der Mandant muss deshalb gesetzt sein, bevor
 * die Transaktion beginnt.
 */
public final class MandantKontext {

    private static final ScopedValue<UUID> MANDANT = ScopedValue.newInstance();

    private MandantKontext() {}

    public static Optional<UUID> aktuell() {
        return MANDANT.isBound() ? Optional.of(MANDANT.get()) : Optional.empty();
    }

    public static UUID erforderlich() {
        return aktuell()
                .orElseThrow(() -> new FachlicherFehler(
                        AllgemeinerFehlercode.MANDANT_FEHLT,
                        "Kein Mandant gewählt. Bei mehreren Mandanten den Header "
                                + MandantFilter.HEADER
                                + " mitsenden."));
    }

    /** Mandanten, die das Token des angemeldeten Benutzers erlaubt; leer ohne Token. */
    public static List<UUID> erlaubteMandanten() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            return MandantClaim.lesen(token.getToken());
        }
        return List.of();
    }

    public static void ausfuehrenFuer(UUID mandantId, Runnable aktion) {
        ScopedValue.where(MANDANT, Objects.requireNonNull(mandantId, "mandantId"))
                .run(aktion);
    }

    public static <T, X extends Throwable> T berechnenFuer(
            UUID mandantId, ScopedValue.CallableOp<? extends T, X> aktion) throws X {
        return ScopedValue.where(MANDANT, Objects.requireNonNull(mandantId, "mandantId"))
                .call(aktion);
    }
}
