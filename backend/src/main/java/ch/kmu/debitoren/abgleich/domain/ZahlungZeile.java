package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Eine Zahlung für Klärungsliste und Übersicht, mit dem Rest, der noch keiner Rechnung zugeordnet ist.
 */
public record ZahlungZeile(
        UUID id,
        long version,
        UUID zahlungsdateiId,
        String bankReferenz,
        LocalDate buchungsdatum,
        LocalDate valuta,
        Geld betrag,
        Geld rest,
        String referenz,
        String auftraggeber,
        String auftraggeberIban,
        String mitteilung,
        Zahlungsstatus status,
        Klaerungsgrund klaerungsgrund) {}
