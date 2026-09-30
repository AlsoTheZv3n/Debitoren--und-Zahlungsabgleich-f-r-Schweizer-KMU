package ch.kmu.debitoren.stammdaten.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port für Mandanten. Mandanten tragen selbst keine {@code mandant_id} und sind nicht gefiltert. */
public interface MandantRepository {

    /** Mandanten mit den gegebenen IDs, nach Name sortiert. */
    List<Mandant> findeAlle(Collection<UUID> ids);

    Optional<Mandant> finde(UUID id);

    boolean existiert(UUID id);

    /** IDs aller Mandanten, nach Name sortiert. */
    List<UUID> alleIds();

    Mandant speichern(Mandant mandant);
}
