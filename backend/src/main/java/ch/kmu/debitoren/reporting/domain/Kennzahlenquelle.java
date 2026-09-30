package ch.kmu.debitoren.reporting.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port für die Auswertungen. Die Umsetzung liest die berechneten Sichten der Fachmodule direkt aus der Datenbank
 * und muss dabei selbst nach Mandant filtern.
 */
public interface Kennzahlenquelle {

    Uebersicht uebersicht(UUID mandant, LocalDate heute);

    Altersstruktur altersstruktur(UUID mandant, LocalDate heute);

    /** Eingänge ab dem ersten Tag von {@code von}, nur Monate mit Zahlungen, nach Monat und Währung sortiert. */
    List<Monatseingang> zahlungseingaenge(UUID mandant, LocalDate von);
}
