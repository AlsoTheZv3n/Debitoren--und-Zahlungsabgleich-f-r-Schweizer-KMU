package ch.kmu.debitoren.mahnwesen.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für die lesende Sicht {@code v_mahnungen}. */
public interface Mahnungsuebersicht {

    /**
     * Mahnungen nach Status und Rechnung; der Suchtext trifft Rechnungsnummer und Kunde. Vorschläge nach
     * Fälligkeit, alle anderen nach Mahndatum, neueste zuerst.
     */
    Page<MahnungZeile> suche(Mahnungsstatus status, UUID rechnungId, String suchtext, Pageable seite);

    Optional<MahnungZeile> zeile(UUID id);

    /** Alle Mahnungen einer Rechnung, älteste Stufe zuerst. */
    List<MahnungZeile> derRechnung(UUID rechnungId);
}
