package ch.kmu.debitoren.rechnung.domain;

import java.util.Set;
import org.springframework.modulith.NamedInterface;

/**
 * Lebenslauf einer Rechnung. Teilbezahlt und Bezahlt setzt der Abgleich über die Zuordnungen; überfällig ist
 * kein Status, sondern ergibt sich aus Fälligkeit und heutigem Datum.
 */
@NamedInterface("modell")
public enum Rechnungsstatus {
    ENTWURF,
    OFFEN,
    TEILBEZAHLT,
    BEZAHLT,
    STORNIERT;

    public static final Set<Rechnungsstatus> OFFENE = Set.of(OFFEN, TEILBEZAHLT);

    public boolean istOffen() {
        return OFFENE.contains(this);
    }
}
