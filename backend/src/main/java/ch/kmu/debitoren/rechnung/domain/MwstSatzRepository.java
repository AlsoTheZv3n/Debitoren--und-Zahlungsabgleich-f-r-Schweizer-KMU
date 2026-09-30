package ch.kmu.debitoren.rechnung.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port für die MWST-Sätze des Mandanten. */
public interface MwstSatzRepository {

    /** Alle Sätze, nach Code und Beginn sortiert. */
    List<MwstSatz> alle();

    Optional<MwstSatz> gueltigAm(MwstCode code, LocalDate datum);

    boolean vorhanden();

    MwstSatz speichern(MwstSatz satz);
}
