package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für Rechnungen; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface RechnungRepository {

    Rechnung speichern(Rechnung rechnung);

    Optional<Rechnung> finde(UUID id);

    /**
     * Lädt die Rechnung, um ihren Zahlungsstand zu ändern, und erhöht ihre Version beim Commit auch dann,
     * wenn der Status gleich bleibt. Gleichzeitige Zuordnungen zur selben Rechnung scheitern so an der Version.
     */
    Optional<Rechnung> zumVerbuchen(UUID id);

    void loeschen(Rechnung rechnung);

    /** Liste mit offenem Betrag aus {@code v_offene_posten}; {@code heute} entscheidet über «überfällig». */
    Page<RechnungZeile> suche(Rechnungsfilter filter, LocalDate heute, Pageable seite);

    Optional<Posten> posten(UUID id);

    Optional<Posten> postenNachReferenz(Zahlungsreferenz referenz);

    /** Offene und teilbezahlte Rechnungen, deren offener Betrag genau dem Betrag entspricht; älteste zuerst. */
    List<Posten> zahlbareMitOffenemBetrag(Geld betrag);

    /** Alle offenen und teilbezahlten Rechnungen nach Nummer, etwa für Testdateien. */
    Page<Posten> zahlbare(Pageable seite);
}
