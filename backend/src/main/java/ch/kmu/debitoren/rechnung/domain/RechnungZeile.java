package ch.kmu.debitoren.rechnung.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

/** Eine Zeile der Rechnungsliste; der offene Betrag stammt aus der berechneten Sicht der offenen Posten. */
public record RechnungZeile(
        UUID id,
        long version,
        String rechnungsnummer,
        Rechnungsstatus status,
        UUID kundeId,
        String kundeName,
        LocalDate rechnungsdatum,
        LocalDate faelligAm,
        Currency waehrung,
        BigDecimal total,
        BigDecimal offen) {

    /** Ohne Zeile in der Sicht der offenen Posten ist nichts offen. */
    public RechnungZeile {
        offen = offen == null ? BigDecimal.ZERO.setScale(2) : offen;
    }

    public boolean istUeberfaellig(LocalDate heute) {
        return status.istOffen() && faelligAm.isBefore(heute);
    }
}
