package ch.kmu.debitoren.zahlungsimport.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Port für Zahlungen; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface ZahlungRepository {

    void speichernAlle(List<Zahlung> zahlungen);

    Optional<Zahlung> finde(UUID id);

    /**
     * Lädt die Zahlung für eine Änderung und erhöht ihre Version beim Commit, auch wenn sich sonst nichts
     * ändert. Zwei gleichzeitige Zuordnungen derselben Zahlung scheitern so an der Version.
     */
    Optional<Zahlung> zumAendern(UUID id);

    /** Welche der Bankreferenzen schon zu einer Zahlung gehören. */
    Set<String> bekannteBankreferenzen(Collection<String> bankReferenzen);

    long anzahlMitStatus(Zahlungsstatus status);
}
