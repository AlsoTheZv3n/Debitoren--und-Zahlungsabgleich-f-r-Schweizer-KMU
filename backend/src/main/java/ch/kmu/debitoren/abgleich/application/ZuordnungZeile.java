package ch.kmu.debitoren.abgleich.application;

import ch.kmu.debitoren.abgleich.domain.Zuordnungsart;
import ch.kmu.debitoren.shared.domain.Geld;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Eine Zuordnung mit dem Nötigsten zu Zahlung und Rechnung, für Rechnungsansicht und Klärungsliste. */
public record ZuordnungZeile(
        UUID id,
        UUID zahlungId,
        UUID rechnungId,
        String rechnungsnummer,
        String kundeName,
        Geld betrag,
        Zuordnungsart art,
        String regel,
        String erstelltVon,
        Instant erstelltAm,
        LocalDate valuta,
        String auftraggeber) {}
