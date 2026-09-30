package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.time.LocalDate;
import java.util.UUID;

/** Eine überfällige Rechnung aus Sicht des Mahnwesens. */
public record MahnbareRechnung(
        UUID rechnungId, String rechnungsnummer, UUID kundeId, String kundeName, LocalDate faelligAm, Geld offen) {}
