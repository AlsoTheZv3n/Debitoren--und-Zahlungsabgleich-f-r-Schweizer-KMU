package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für die lesende Sicht auf Zahlungen mit berechnetem Rest ({@code v_zahlungen}). */
public interface Zahlungsuebersicht {

    /**
     * Neueste Valuta zuerst; {@code null} heisst jeweils «ohne Einschränkung».
     *
     * @param suchtext sucht in Auftraggeber, Referenz, Mitteilung und Bankreferenz
     */
    Page<ZahlungZeile> suche(Zahlungsstatus status, UUID zahlungsdateiId, String suchtext, Pageable seite);

    Optional<ZahlungZeile> zeile(UUID id);
}
