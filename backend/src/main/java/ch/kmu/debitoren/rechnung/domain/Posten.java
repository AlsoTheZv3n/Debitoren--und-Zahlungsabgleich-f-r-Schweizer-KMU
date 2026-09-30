package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Eine freigegebene Rechnung aus Sicht des Zahlungseingangs und des Mahnwesens: was sie kostet, was noch offen
 * ist und unter welcher Referenz sie bezahlt wird. Der offene Betrag stammt aus der Sicht {@code v_offene_posten}.
 *
 * @param kundeName Name des Empfängers auf der Rechnung
 * @param referenz QR- oder SCOR-Referenz ohne Leerzeichen
 * @param mahngebuehren Gebühren aus freigegebenen Mahnungen; sie gehören zur Forderung
 * @param offen Total plus Mahngebühren abzüglich Zahlungseingänge; null, wenn die Rechnung bezahlt oder
 *     storniert ist
 */
@NamedInterface("modell")
public record Posten(
        UUID rechnungId,
        String rechnungsnummer,
        Rechnungsstatus status,
        UUID kundeId,
        String kundeName,
        String referenz,
        LocalDate faelligAm,
        Geld total,
        Geld mahngebuehren,
        Geld offen) {

    /** Für Abfragen, die Rechnung und offene Posten verbinden. */
    public Posten(
            UUID rechnungId,
            String rechnungsnummer,
            Rechnungsstatus status,
            UUID kundeId,
            String kundeName,
            Zahlungsreferenz referenz,
            LocalDate faelligAm,
            Currency waehrung,
            BigDecimal total,
            BigDecimal mahngebuehren,
            BigDecimal offen) {
        this(
                rechnungId,
                rechnungsnummer,
                status,
                kundeId,
                kundeName,
                referenz == null ? null : referenz.wert(),
                faelligAm,
                new Geld(total, waehrung),
                new Geld(mahngebuehren, waehrung),
                new Geld(offen == null ? BigDecimal.ZERO : offen, waehrung));
    }

    /** Offen oder teilbezahlt: Die Rechnung nimmt noch Zahlungen an. */
    public boolean istZahlbar() {
        return status.istOffen() && offen.istPositiv();
    }

    /** Summe der Zahlungseingänge: was von Total und Gebühren nicht mehr offen ist; nicht für Stornierte. */
    public Geld bezahlt() {
        return total.plus(mahngebuehren).minus(offen);
    }
}
