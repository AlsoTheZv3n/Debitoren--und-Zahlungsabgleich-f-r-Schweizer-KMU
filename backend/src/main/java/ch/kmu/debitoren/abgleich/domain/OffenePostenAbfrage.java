package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port zu den Rechnungen: nur was die Zuordnungsstrategien und die Klärungsliste brauchen. */
public interface OffenePostenAbfrage {

    Optional<OffenerPosten> finde(UUID rechnungId);

    /** Die freigegebene Rechnung mit dieser Referenz, gleich ob offen, bezahlt oder storniert. */
    Optional<OffenerPosten> nachReferenz(String referenz);

    /** Zahlbare Rechnungen, bei denen genau dieser Betrag offen ist; älteste Fälligkeit zuerst. */
    List<OffenerPosten> zahlbareMitOffenemBetrag(Geld betrag);
}
