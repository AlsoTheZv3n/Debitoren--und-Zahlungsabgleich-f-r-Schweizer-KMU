package ch.kmu.debitoren.abgleich.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port für Zuordnungen; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface ZuordnungRepository {

    Zuordnung speichern(Zuordnung zuordnung);

    Optional<Zuordnung> finde(UUID id);

    void loeschen(Zuordnung zuordnung);

    /** Älteste zuerst. */
    List<Zuordnung> fuerZahlung(UUID zahlungId);

    /** Älteste zuerst. */
    List<Zuordnung> fuerRechnung(UUID rechnungId);
}
