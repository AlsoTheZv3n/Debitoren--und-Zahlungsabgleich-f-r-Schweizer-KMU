package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Eine freigegebene Rechnung, wie der Abgleich sie sieht.
 *
 * @param kundeName Name des Empfängers auf der Rechnung, für den Vergleich mit dem Auftraggeber
 * @param offen noch offener Betrag; null bei bezahlten oder stornierten Rechnungen
 */
public record OffenerPosten(
        UUID rechnungId,
        String rechnungsnummer,
        String kundeName,
        Postenzustand zustand,
        LocalDate faelligAm,
        Geld total,
        Geld offen) {

    public boolean istZahlbar() {
        return zustand == Postenzustand.ZAHLBAR;
    }
}
