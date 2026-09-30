package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.MwstCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Eingaben für einen Rechnungsentwurf. Ohne Zahlungsfrist gilt die des Kunden; die MWST-Sätze werden für
 * das Rechnungsdatum aufgelöst.
 */
public record Entwurf(
        UUID kundeId,
        LocalDate rechnungsdatum,
        Integer zahlungsfristTage,
        Currency waehrung,
        String bemerkung,
        List<Position> positionen) {

    public Entwurf {
        positionen = positionen == null ? List.of() : List.copyOf(positionen);
    }

    public record Position(
            String beschreibung, BigDecimal menge, String einheit, BigDecimal einzelpreis, MwstCode mwstCode) {}
}
