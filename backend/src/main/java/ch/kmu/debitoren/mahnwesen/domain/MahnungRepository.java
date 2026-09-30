package ch.kmu.debitoren.mahnwesen.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port für Mahnungen; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface MahnungRepository {

    Mahnung speichern(Mahnung mahnung);

    Optional<Mahnung> finde(UUID id);

    /** Alle Mahnungen dieser Rechnungen, auch verworfene. */
    List<Mahnung> fuerRechnungen(Collection<UUID> rechnungIds);

    /** Alle Vorschläge, die auf Freigabe warten. */
    List<Mahnung> vorschlaege();

    /** Vermerkt das abgelegte Schreiben, ohne die Version zu prüfen oder zu erhöhen. */
    void pdfAbgelegt(UUID id, String schluessel);
}
